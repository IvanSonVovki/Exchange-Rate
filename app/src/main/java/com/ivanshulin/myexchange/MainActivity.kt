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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var pref: SharedPreferences
    private lateinit var spinner: Spinner
    private lateinit var amount: EditText
    private lateinit var resultConvert: TextView
    private lateinit var btnUpdate: Button
    private lateinit var currencySymbol: TextView
    private lateinit var spinnerValute: ArrayAdapter<String>
    private lateinit var recyclerView: RecyclerView

    private var timer = Timer()
    private var valuteList = listOf<String>()
    private var exchangeRateList = listOf<ExchangeRate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "start ${exchangeRateList.size}")

        pref = getSharedPreferences("DATE", MODE_PRIVATE) //созданние базы данных

        /** инициализация View **/
        recyclerView = findViewById(R.id.recycler_view)

        btnUpdate = findViewById(R.id.btn_update)
        spinner = findViewById(R.id.sp_valute)
        amount = findViewById(R.id.et_amount)
        resultConvert = findViewById(R.id.tv_result_convert)
        currencySymbol = findViewById(R.id.tv_char_ed_right)

        //обновление данных если отсутствует сохраннение в БД
        if (!pref.contains(Key.JSON_STRING.key)) updateData()

        // получение Json в формате String из БД
        val jsonString = pref.getString(
            Key.JSON_STRING.key,
            fetchJsonString()
        )

        Log.d(TAG, "end ${exchangeRateList.size}")

        if (isInternetAvailable(this)) {
            autoUpdate() // запуск автообноления
        }

        // ручное обновление
        btnUpdate.setOnClickListener {
            timer.cancel()
            timer = Timer()

            if (isInternetAvailable(this)) {
                updateData()
                autoUpdate()
            } else {
                showToast("No internet")
            }
        }

        // заполнение списка валют с данными
        exchangeRateList = getData(jsonString ?: fetchJsonString()) ?: emptyList()

        updateRecycler() // загрузка списка данных для отображение в RecyclerView

        valuteList = exchangeRateList.map { it.charCode }  // получение списка валют для Spinner
        btnUpdate.text = pref.getString(Key.CURRENT_DATE.key, getCurrentDate())
            ?: getCurrentDate() // отображение даты обновления

        spinnerValute = ArrayAdapter(this, android.R.layout.simple_spinner_item, valuteList)
        spinnerValute.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = spinnerValute
        spinner.onItemSelectedListener = this

        amount.setOnClickListener {
            amount.text.clear()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // обработка полученной в спиннере валюты

        val rate = exchangeRateList.find { valuteList[pos] == it.charCode }
        val converterResult = getValueData(checkNotNull(rate), amount.text.toString())

//        amount.setOnKeyListener(KeyListener(converterResult))
        amount.setOnKeyListener { editText, code, event ->
            if (event?.action == KeyEvent.ACTION_DOWN &&
                code == KeyEvent.KEYCODE_ENTER
            ) {
                resultConvert.text = converterResult.result
                currencySymbol.text = converterResult.symbol
                resultConvert.clearFocus()
                resultConvert.isCursorVisible = false
                handleKeyEvent(editText!!, code)
                return@setOnKeyListener true
            }
            false
        }
        resultConvert.text = converterResult.result
        currencySymbol.text = converterResult.symbol
        amount.setText(converterResult.rubles)

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun updateData() {
        if (isInternetAvailable(this)) {
            val currentDate = getCurrentDate()
            val jsonString = fetchJsonString()
            val data = getData(jsonString)

            btnUpdate.text = currentDate
            if (data != null) {
                exchangeRateList = data
                saveData(jsonString, currentDate)
            } else {
                showToast("Incorrect JSON from server!")
            }

            updateRecycler()
        }
    }

    private fun handleKeyEvent(view: View, keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // скрыть клавиатуру
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            return true
        }
        return false
    }

    private fun updateRecycler() {
        recyclerView.layoutManager = LinearLayoutManager(MainActivity())
        recyclerView.adapter = CustomRecyclerAdapter(exchangeRateList)
    }

    private fun saveData(jsString: String, currentDate: String) {
        pref.edit().apply {
            putString(Key.JSON_STRING.key, jsString)
            putString(Key.CURRENT_DATE.key, currentDate)
            apply()
        }
    }

    private fun autoUpdate() {
        val updatePeriodMinutes = 0.1
        val updatePeriod = (60 * 1000 * updatePeriodMinutes).toLong()

        val timerTask = object : TimerTask() {
            override fun run() {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) { updateData() }
                }
            }
        }
        timer.schedule(timerTask, updatePeriod, updatePeriod)
        Log.d(TAG, "autoUpdate: <->")
    }

    private fun getCurrentDate(): String {
        val time = Date()
        val dateFormat = SimpleDateFormat("dd MMMM HH:mm:ss", Locale.ENGLISH)

        return dateFormat.format(time)
    }

    private fun fetchJsonString(): String = runBlocking {
        Log.e(TAG, "fetchJsonString: jsonAccessed")
        val url = "https://www.cbr-xml-daily.ru/daily_json.js"
        var serverResponse = ""
        if (isInternetAvailable(this@MainActivity)) {
            serverResponse = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
        } else showToast("No internet")
        serverResponse
    }

    private fun showToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 200)
        toast.show()
    }

    inner class KeyListener(private val result: ConverterResult) : View.OnKeyListener {

        override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
            if (event?.action == KeyEvent.ACTION_DOWN &&
                keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                resultConvert.text = result.result
                currencySymbol.text = result.symbol
                resultConvert.clearFocus()
                resultConvert.isCursorVisible = false
                handleKeyEvent(v!!, keyCode)
                return true
            }
            return false
        }

    }

    companion object {
        val TAG = "${MainActivity::class.java.simpleName}_TAG"
    }

}