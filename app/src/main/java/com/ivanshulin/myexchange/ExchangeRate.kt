package com.ivanshulin.myexchange

import android.util.Log
import com.ivanshulin.myexchange.MainActivity.Companion.TAG
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

data class ExchangeRate(
    val id: String,
    val numCode: String,
    val charCode: String,
    val nominal: String,
    val name: String,
    val value: String,
    val previous: String,
    val date: String
)

fun getData(jsonString: String?): List<ExchangeRate>? = try {
    val jsonResponse = JSONTokener(jsonString).nextValue() as JSONObject
    val jsonValute = jsonResponse.getJSONObject("Valute")

    val jsonObjectList = jsonValute.names()

    val resultList = mutableListOf<ExchangeRate>()
    if (jsonObjectList != null) {
        for (i in 0 until jsonObjectList.length()) {

            val valute = jsonValute.getJSONObject(jsonObjectList[i].toString())
            resultList.add(
                ExchangeRate(
                    id = valute.getString("ID"),
                    numCode = valute.getString("NumCode"),
                    charCode = valute.getString("CharCode"),
                    nominal = valute.getString("Nominal"),
                    name = valute.getString("Name"),
                    value = valute.getString("Value"),
                    previous = valute.getString("Previous"),
                    date = jsonResponse.getString("Date")
                )
            )

        }
    }
    Log.d(TAG, resultList[0].toString())

    resultList.toList()
} catch (exception: JSONException) {
    null
}