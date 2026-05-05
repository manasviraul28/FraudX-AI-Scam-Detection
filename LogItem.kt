package com.fraudx.app

data class LogItem(
    val type: String,
    val number: String,
    val preview: String,
    val risk: String,
    val timestamp: Long
)