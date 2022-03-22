package com.ivanshulin.myexchange

import java.math.BigDecimal
import java.util.*

fun getValuteListData(valute: String, _amount: String): List<String> {

     var returnList = mutableListOf<String>("","","")

     var amount = _amount.replace(',', '.').toDouble()      //сумма в рублях
     var nominalFirst = BigDecimal("1.0000")
     var value = BigDecimal("0.0000")
     var costUnit = BigDecimal("1.0000")
     var resultConvert = BigDecimal("0.0000")

     exchangeRateList.forEach {
        if (valute == it.charCode) {
            nominalFirst = it.nominal.toBigDecimal()
            costUnit = it.value.toBigDecimal() / nominalFirst
        }
    }
    if (amount == 0.0) {
        val result = BigDecimal("1.0000") / costUnit
        returnList[0] = "1"
        returnList[1] = "%.2f".format(result)
    } else {
        resultConvert = amount.toBigDecimal() / costUnit
        returnList[0] = "%.2f".format(amount)
        returnList[1] = "%.2f".format(resultConvert)
    }
    val symbolCurrency = Currency.getInstance(valute).symbol
    returnList[2] = if (symbolCurrency.length > 1)"\u2BD1" else symbolCurrency



    return returnList.toList()
 }