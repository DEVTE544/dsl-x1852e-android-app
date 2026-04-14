package com.example.d_linkmobilymanagement.ui.utils

fun String.normalizeForSearch(): String {
    return this.trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
