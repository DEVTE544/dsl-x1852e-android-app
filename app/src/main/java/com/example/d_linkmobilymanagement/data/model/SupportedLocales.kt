package com.example.d_linkmobilymanagement.data.model

import com.example.d_linkmobilymanagement.R

data class LocaleInfo(
    val tag: String,
    val nameResId: Int
)

object SupportedLocales {
    val locales = listOf(
        LocaleInfo("ar", R.string.language_ar),
        LocaleInfo("en", R.string.language_en)
    )
}
