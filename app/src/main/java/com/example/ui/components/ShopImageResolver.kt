package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.ShopFacility
import kotlin.math.abs

object ShopImageResolver {
    private val curatedGallery = listOf(
        R.drawable.img_cafe_midnight,
        R.drawable.img_cafe_roastery,
        R.drawable.img_cafe_lounge,
        R.drawable.img_cafe_espresso,
        R.drawable.img_cafe_terrace,
        R.drawable.img_cafe_rooftop
    )

    @DrawableRes
    fun resolveDrawable(shop: CoffeeShop): Int {
        if (shop.imageRes != 0) return shop.imageRes
        val facilities = shop.resolvedFacilities
        val hash = abs(shop.id.ifBlank { shop.name }.hashCode())
        return when {
            ShopFacility.ROOFTOP in facilities && hash % 2 == 0 -> R.drawable.img_cafe_rooftop
            ShopFacility.ROOFTOP in facilities -> R.drawable.img_cafe_terrace
            shop.isOpenAfterMidnight && hash % 3 == 0 -> R.drawable.img_cafe_midnight
            ShopFacility.QUIET in facilities && hash % 2 == 0 -> R.drawable.img_cafe_lounge
            else -> curatedGallery[hash % curatedGallery.size]
        }
    }
}
