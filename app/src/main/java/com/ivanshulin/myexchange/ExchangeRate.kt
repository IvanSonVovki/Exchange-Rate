package com.ivanshulin.myexchange

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