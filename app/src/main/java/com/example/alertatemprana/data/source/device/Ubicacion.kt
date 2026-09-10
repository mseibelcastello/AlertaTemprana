package com.example.alertatemprana.data.source.device

import android.content.Context
import android.location.LocationManager

class Ubicacion(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun obtenerUltima(): Pair<Double, Double>? {
        return try {
            val gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val red = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val mejor = gps ?: red
            if (mejor != null) mejor.latitude to mejor.longitude else null
        } catch (_: Exception) {
            null
        }
    }
}