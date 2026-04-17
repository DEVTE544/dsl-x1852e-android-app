package com.example.d_linkmobilymanagement.ui.state

sealed interface UiEvent {
    data class ShowMessage(
        val messageRes: Int,
        val args: List<Any>? = null,
        val isPlural: Boolean = false,
        val pluralQuantity: Int = 0
    ) : UiEvent
}
