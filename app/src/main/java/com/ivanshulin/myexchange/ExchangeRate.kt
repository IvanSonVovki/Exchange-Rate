package com.ivanshulin.myexchange

import android.util.Log
import org.json.JSONObject
import org.json.JSONTokener

data class ExchangeRate (
    val id: String,
    val numCode: String,
    val charCode: String,
    val nominal: String,
    val name: String,
    val value: String,
    val previous: String,
    val date: String
)

fun date(jsonString: String): List<ExchangeRate> {

    val jsonObject = JSONTokener(jsonString).nextValue() as JSONObject

    val jsonObject2 = jsonObject.getJSONObject("Valute")

    val jsonObjectList = jsonObject2.names()
    jsonValuteList = jsonObjectList

    exchangeRateList = mutableListOf<ExchangeRate>()

    // Log.d("myLog", "${jsonObject2.getString()}")
    var resultList = mutableListOf<ExchangeRate>()
    for (i in 0 until jsonObjectList.length()) {


        val valute = jsonObject2.getJSONObject(jsonObjectList[i].toString())
        resultList.add(
                ExchangeRate(
                        id = valute.getString("ID"),
                        numCode = valute.getString("NumCode"),
                        charCode = valute.getString("CharCode"),
                        nominal = valute.getString("Nominal"),
                        name = valute.getString("Name"),
                        value = valute.getString("Value"),
                        previous = valute.getString("Previous"),
                        date = jsonObject.getString("Date")
                )
        )
    }
    Log.d("myLog", resultList[0].toString())

    return resultList.toList()
}