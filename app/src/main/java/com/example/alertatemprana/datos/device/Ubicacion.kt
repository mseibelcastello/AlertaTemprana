package com.example.alertatemprana.datos.device

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper

class Ubicacion(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var listener: LocationListener? = null

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

    fun iniciarSeguimiento(callback: (Location) -> Unit) {
        detenerSeguimiento()
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                callback(location)
            }
        }
        val l = listener ?: return
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { locationManager.isProviderEnabled(it) }
            .forEach { provider ->
                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        2000L,
                        0f,
                        l,
                        Looper.getMainLooper()
                    )
                } catch (_: SecurityException) {
                }
            }
    }

    fun detenerSeguimiento() {
        listener?.let { locationManager.removeUpdates(it) }
        listener = null
    }
}