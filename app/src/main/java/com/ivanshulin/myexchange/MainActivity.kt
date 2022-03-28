package com.ivanshulin.myexchange

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


lateinit var pref: SharedPreferences
var exchangeRateList = listOf<ExchangeRate>()
var jsonValuteList: JSONArray? = null
lateinit var spinner: Spinner
lateinit var amount: EditText
lateinit var resultConvert: TextView
lateinit var btnUpdate: Button
lateinit var context: Context
lateinit var showContext: Context
lateinit var tvCurrencyRight: TextView
lateinit var spinnerValute: ArrayAdapter<String>
lateinit var recyclerView: RecyclerView
var timer = Timer()

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    var valuteList = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        showContext = applicationContext

        Log.d("myLog", "начало ${exchangeRateList.size}")
        // Log.d("myLog", "${exchangeRateList[0].toString()}")

        pref = getSharedPreferences("DATE", MODE_PRIVATE) //созданние базы данных

            /** инициализация View **/
        recyclerView = findViewById(R.id.recycler_view)

        btnUpdate = findViewById(R.id.btn_update)
        spinner = findViewById(R.id.sp_valute)
        amount = findViewById(R.id.et_amount)
        resultConvert = findViewById(R.id.tv_result_convert)
        tvCurrencyRight = findViewById(R.id.tv_char_ed_right)


        if (!pref.contains(Key.JSON_STRING.key)) getBtnUpdate()//обновление данных если отсутствует сохраннение в БД
        val jsStr = pref.getString(Key.JSON_STRING.key, getJsounString()) // получение Json в формате String из БД

        Log.d("myTag", "end = ${exchangeRateList.size}")

        if (isOnline(this)) {
            autoUpdate() // запуск автообноления
        }


         // ручное обновление
        btnUpdate.setOnClickListener {

            if (timer != null) {
                timer.cancel()
                timer = Timer()

            }
            getBtnUpdate()
            autoUpdate()
        }


        exchangeRateList = date(jsStr ?: getJsounString()) // заполнение списка валют с данными

        updateRecycler() // загрузка списка данных для отображение в RecyclerView

        valuteList = getValuteList(exchangeRateList)  // получение списка валют для Spinner
        btnUpdate.text = pref.getString(Key.CURRENT_DATE.key, getCurrentDate())?: getCurrentDate()// отображение даты обновления

        spinnerValute = ArrayAdapter(this, android.R.layout.simple_spinner_item, valuteList)
        spinnerValute.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.setAdapter(spinnerValute)
        spinner.onItemSelectedListener = this

        amount.setOnClickListener {
            amount.text.clear()
        }
    }

    private fun handleKeyEvent(view: View, keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Hide the keyboard
            val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            return true
        }
        return false
    }


//получаю валюту выбранную в Spinner
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {

        fun creativeListDate(): List<String> {
            return getValuteListData(valuteList[position], amount.text.toString()?: "1.0")

        }

        amount.setOnKeyListener(object : View.OnKeyListener {

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

                if (event?.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    val listDate = creativeListDate()
                    resultConvert.text = listDate[1]
                    tvCurrencyRight.text = listDate[2]

                    resultConvert.clearFocus()
                    resultConvert.isCursorVisible = false
                    handleKeyEvent(v!!, keyCode)
                    return true
                }
                return false
            }
        })
        resultConvert.text = creativeListDate()[1]
    tvCurrencyRight.text = creativeListDate()[2]
        amount.setText(creativeListDate()[0])

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}
fun updateRecycler() {
    recyclerView.layoutManager = LinearLayoutManager(MainActivity())
    recyclerView.adapter = CustomRecyclerAdapter(exchangeRateList)
}

private fun saveDate(jsString: String, currentDate: String) {

    val editor: SharedPreferences.Editor = pref.edit()
    editor.putString(Key.JSON_STRING.key, jsString)
    editor.putString(Key.CURRENT_DATE.key, currentDate)
    editor.apply()
}
fun autoUpdate(){
     val timeUpdateMinute = 5L
     val timeUpdate: Long = 60 * 1000 * timeUpdateMinute

    timer.schedule(
        object : TimerTask() {

            override fun run() {

                CoroutineScope(Dispatchers.Main).launch {

                    getBtnUpdate()
                }

            }
        },
        timeUpdate,
        timeUpdate
    )
    Log.d("testUpdate","1${timer== null}")
 }

fun getBtnUpdate() {

if (context != null && isOnline(context)) {


    val currentDate = getCurrentDate()
    btnUpdate.text = currentDate
    val jsonString = getJsounString()
    exchangeRateList = date(jsonString)

    saveDate(jsonString, currentDate)
    updateRecycler()
} else {
    var toast = Toast.makeText(showContext, "Connection error!",Toast.LENGTH_LONG)

    toast.setGravity(Gravity.TOP, 0, 200)
    toast.show()
}


}

fun getCurrentDate(): String {
    val time = Date()
    val sdf = SimpleDateFormat("dd MMMM HH:mm:ss")

    return sdf.format(time)
}

fun getValuteList(list: List<ExchangeRate>): List<String> {
    var resultList = mutableListOf<String>()
    for (obj in list) {
        resultList.add(
            obj.charCode
        )
    }
    return resultList.toList()
}

fun getJsounString(): String {

    val url = "https://www.cbr-xml-daily.ru/daily_json.js"
    var jsReturnString = ""
    if (context != null && isOnline(context)) {
        val globalJob = GlobalScope.launch {
            jsReturnString = URL(url).readText()
        }
        runBlocking {
            globalJob.join()
        }
    } else {
//        Toast.makeText(MainActivity(), "Connection Error!",Toast.LENGTH_SHORT).show()

    }

    return jsReturnString
}

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

enum class Key(val key: String) {
    CURRENT_DATE("currentDate"),
    JSON_STRING("jsonString")
}


