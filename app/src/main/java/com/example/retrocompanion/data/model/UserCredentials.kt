package com.example.retrocompanion.data.model

data class UserCredentials(
    val username: String = "",
    val apiKey: String = ""
) {
    val isValid: Boolean
        get() = username.isNotBlank() && apiKey.isNotBlank()
}
