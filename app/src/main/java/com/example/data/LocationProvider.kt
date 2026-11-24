package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationProvider {

    fun hasCoarseLocationPermission(context: Context): Boolean {
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarseGranted || fineGranted
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double>? {
        if (!hasCoarseLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            val location = runCatching {
                if (manager.isProviderEnabled(provider)) {
                    manager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }.getOrNull() ?: continue

            if (bestLocation == null || location.time > bestLocation.time) {
                bestLocation = location
            }
        }

        // Fall back to any cached provider even if currently toggled off
        if (bestLocation == null) {
            for (provider in providers) {
                val cached = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
                if (bestLocation == null || cached.time > bestLocation.time) {
                    bestLocation = cached
                }
            }
        }

        return bestLocation?.let { it.latitude to it.longitude }
    }
}
