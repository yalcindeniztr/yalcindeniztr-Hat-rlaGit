package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val customFields: List<String> = emptyList()
)
