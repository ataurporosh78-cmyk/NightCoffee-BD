package com.example.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Immutable
@Keep
data class MenuItem(
    val name: String = "",
    val description: String = "",
    val priceBdt: Int = 0,
    val isHouseFavorite: Boolean = false
)
