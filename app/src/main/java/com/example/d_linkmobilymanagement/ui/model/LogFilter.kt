package com.example.d_linkmobilymanagement.ui.model

import com.example.d_linkmobilymanagement.R

enum class LogFilter(val labelResId: Int, val types: List<LogType>?) {
    ALL(R.string.filter_all, null),
    LOGIN(R.string.filter_login, listOf(LogType.LOGIN)),
    REFRESH(R.string.filter_refresh, listOf(LogType.REFRESH)),
    BLOCKING(R.string.filter_blocking, listOf(LogType.BLOCK, LogType.UNBLOCK)),
    SYSTEM(R.string.filter_system, listOf(LogType.SYSTEM, LogType.INFO)),
    ERRORS(R.string.filter_errors, listOf(LogType.ERRORS))
}

