# Exchange-Rate
Android application for calculattiong the exchahge rate measurement according to the Central Bank of the Russian Federation

Приложение для отображения курса валют на основе данных сайта ЦБ РФ.
Вывод списка валют и их курса реализован с помощью RecyclerView, полученые данные сохранены в формате стринг с применением sharedpreferences, разбор json строки реализованно с помощью библеотек JSONObject, JSONTokener. Обновление данных происходит при нажатие на кнопку, или автоматически (5 минут).
