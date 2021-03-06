package com.ivanshulin.myexchange

import android.util.Log
import com.ivanshulin.myexchange.MainActivity.Companion.TAG
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

val decimalFormatSymbols = DecimalFormatSymbols(Locale.ENGLISH)
val DISPLAY_FORMAT = DecimalFormat("#.##", decimalFormatSymbols)

class ConverterResult(val rubles: String, val result: String, val symbol: String)

fun getValueData(valute: ExchangeRate, amount: String): ConverterResult {
    // определяем знак валюты если есть
    val symbolCurrency = Currency.getInstance(valute.charCode).symbol
    val symbol = if (symbolCurrency.length > 1) "\u2BD1" else symbolCurrency

    try {
        val rublesFieldText =
            if (amount.isEmpty()) 1.0
            else amount.replace(",", ".").toDouble()

        val nominalFirst: BigDecimal = valute.nominal.toBigDecimal()
        val costUnit = valute.value.toBigDecimal() / nominalFirst
        val resultConvert: BigDecimal

        val sumRubles: String
        val sumCurrency: String
        if (rublesFieldText == 0.0) {
            val value = BigDecimal("1.0000") / costUnit
            sumRubles = "1"
            sumCurrency = DISPLAY_FORMAT.format(value)
        } else {
            resultConvert = rublesFieldText.toBigDecimal() / costUnit
            sumRubles = DISPLAY_FORMAT.format(rublesFieldText)
            sumCurrency = DISPLAY_FORMAT.format(resultConvert)
        }

        Log.e(TAG, "getValueData: $sumRubles = $sumCurrency (${valute.charCode})")

        return ConverterResult(sumRubles, sumCurrency, symbol)
    } catch (exception: NumberFormatException) {
        return ConverterResult("0", "Error", symbol)
    }
}