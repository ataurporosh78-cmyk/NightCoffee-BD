package com.example.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Immutable
@Keep
data class MenuItem(
    val name: String = "",
    val priceBdt: Int = 0,
    val description: String = "",
    val isHouseFavorite: Boolean = false
)
