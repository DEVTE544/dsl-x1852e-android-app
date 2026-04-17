package com.example.d_linkmobilymanagement.ui.model

import java.util.Date

data class LogUiModel(
    val id: String,
    val timestamp: Date,
    val type: LogType,
    val messageRes: Int,
    val messageArgs: List<Any>? = null,
    val isSuccess: Boolean = true,
    val isPlural: Boolean = false,
    val pluralQuantity: Int = 0
)

enum class LogType {
    LOGIN,
    REFRESH,
    BLOCK,
    UNBLOCK,
    UPDATE_META,
    SYSTEM,
    ERRORS,
    INFO
}
