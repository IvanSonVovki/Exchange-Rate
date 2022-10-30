package com.ivanshulin.myexchange

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
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var pref: SharedPreferences
    private lateinit var spinner: Spinner
    private lateinit var amount: EditText
    private lateinit var resultConvert: EditText
    private lateinit var btnUpdate: Button
    private lateinit var currencySymbol: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var recyclerView: RecyclerView

    private var timer = Timer()
    private var valuteList = listOf<String>()
    private var exchangeRateList = listOf<ExchangeRate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // получение preferences или созданние если их не было
        pref = getSharedPreferences("DATE", MODE_PRIVATE)

        /** инициализация View **/
        recyclerView = findViewById(R.id.recycler_view)

        btnUpdate = findViewById(R.id.btn_update)
        spinner = findViewById(R.id.sp_valute)
        amount = findViewById(R.id.et_amount)
        resultConvert = findViewById(R.id.et_convert_result)
        currencySymbol = findViewById(R.id.tv_char_ed_right)

        /*
        * Запуск корутины в контексте жизненного цикла активити,
        * корутина не блокирует текущий поток. То есть то что происходит в блоке launch {}
        * будет выполняться парллельно с тем что идёт после
        * */
        lifecycleScope.launch {
            // получение данных из сети если есть интренет, иначе из preferences
            val jsonString = if (isInternetAvailable(this@MainActivity))
                saveFetchJsonString()
            else
                pref.getString(Key.JSON_STRING.key, null)

            // парсинг json в список валют
            exchangeRateList = getData(jsonString) ?: emptyList()
            Log.d(TAG, "size of list ${exchangeRateList.size}")

            // подготовка списка валют для спинера
            valuteList = exchangeRateList.map { it.charCode }

            // настройка адаптера для спинера
            val resId = android.R.layout.simple_spinner_item
            adapter = ArrayAdapter(this@MainActivity, resId, valuteList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            // отображение даты обновления на кнопке
            btnUpdate.text = pref.getString(Key.CURRENT_DATE.key, getCurrentDate())

            // загрузка списка данных для отображение в RecyclerView
            updateRecycler()
        }

        // Программа продолжает выполенеине без остановки или ожидания.

        if (isInternetAvailable(this@MainActivity)) {
            launchAutoUpdate() // запуск автообноления
        }

        // Весим слушатели на кнопку, спиннер и поле ввода

        btnUpdate.setOnClickListener {
            timer.cancel()
            timer = Timer()

            if (isInternetAvailable(this@MainActivity)) {
                updateData()
                launchAutoUpdate()
            } else showToast("No internet")
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) = updateEditText()

            override fun onNothingSelected(parent: AdapterView<*>?) = updateEditText()
        }

        amount.setOnKeyListener { editText, code, event ->
            if (code == KeyEvent.KEYCODE_ENTER) {
                amount.clearFocus()
                updateEditText() // обновление текста и поля ввода

                // скрыть клавиатуру
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE)
                val flag = InputMethodManager.RESULT_UNCHANGED_SHOWN

                (inputMethodManager as InputMethodManager)
                    .hideSoftInputFromWindow(amount.windowToken, flag)

                return@setOnKeyListener true
            }
            false
        }

    }

    /**
     * Загружает данные из сети и сохраняет в preferences.
     * Функция, способная приостанавливать своё выполнение помечена модификатором suspend.
     * Внутри неё используется переключение на IO диспетчер, который делегирует выполенение
     * блокирующих операций ввода/вывода пулу потоков.
     */
    fun updateData() = lifecycleScope.launch {
        Log.e(TAG, "updateData: called")

        val jsonString = saveFetchJsonString()
        exchangeRateList = getData(jsonString) ?: emptyList<ExchangeRate>()
            .also { showToast("Incorrect JSON from server") }

        btnUpdate.text = getCurrentDate()
        updateRecycler()

        Log.e(TAG, "updateData: completed")
    }

    private fun updateEditText() {
        val rate = exchangeRateList.find { spinner.selectedItem.toString() == it.charCode }
        val converterResult = getValueData(checkNotNull(rate), amount.text.toString())

        amount.setText(converterResult.rubles)
        resultConvert.setText(converterResult.result)
        currencySymbol.text = converterResult.symbol
    }

    private fun updateRecycler() {
        Log.e(TAG, "updateRecycler: called ${exchangeRateList.size}")
        recyclerView.layoutManager = LinearLayoutManager(MainActivity())
        recyclerView.adapter = CustomRecyclerAdapter(exchangeRateList)
    }

    private fun launchAutoUpdate() {
        val updatePeriodMinutes = 5
        val updatePeriod = (60 * 1000 * updatePeriodMinutes).toLong()

        val timerTask = object : TimerTask() {
            override fun run() {
                lifecycleScope.launch(Dispatchers.Main) { updateData() }
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

    private fun showToast(text: String) {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 200)
        toast.show()
    }

    private fun saveData(data: String, currentDate: String) {
        pref.edit().apply {
            putString(Key.JSON_STRING.key, data)
            putString(Key.CURRENT_DATE.key, currentDate)
            apply()
        }
    }

    /**
     * Загружает данные из сети и сохраняет в preferences.
     * Функция, способная приостанавливать своё выполнение помечена модификатором suspend.
     * Внутри неё используется переключение на IO диспетчер, который делегирует выполенение
     * блокирующих операций ввода/вывода пулу потоков.
     */
    private suspend fun saveFetchJsonString(): String {
        Log.e(TAG, "fetchJsonString: jsonRequested")
        val url = "https://www.cbr-xml-daily.ru/daily_json.js"
        var serverResponse = ""

        @Suppress("BlockingMethodInNonBlockingContext")
        if (isInternetAvailable(this)) {
            // выполнение запроса в другом контексте
            serverResponse = withContext(Dispatchers.IO) { URL(url).readText() }
            // сохранение результата
            saveData(serverResponse, getCurrentDate())
        } else showToast("No internet")

        return serverResponse
    }

    companion object {
        val TAG = "${MainActivity::class.java.simpleName}_TAG"
    }

}