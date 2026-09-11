package com.example.alertatemprana.datos

import com.example.alertatemprana.modelos.ClimaActual
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private fun descripcionTiempo(codigo: Int): String {
    return when (codigo) {
        0 -> "despejado"
        1 -> "mayormente despejado"
        2 -> "parcialmente nublado"
        3 -> "nublado"
        45, 48 -> "niebla"
        51, 53, 55 -> "llovizna"
        56, 57 -> "llovizna helada"
        61 -> "lluvia ligera"
        63 -> "lluvia"
        65 -> "lluvia fuerte"
        66, 67 -> "lluvia helada"
        71 -> "nieve ligera"
        73 -> "nieve"
        75 -> "nieve fuerte"
        77 -> "granos de nieve"
        80 -> "lluvias dispersas"
        81 -> "lluvias"
        82 -> "tormenta de lluvia"
        85 -> "nevadas"
        86 -> "nevadas fuertes"
        95 -> "tormenta"
        96, 99 -> "tormenta con granizo"
        else -> "condición variable"
    }
}

private fun direccionCompass(grados: Int): String {
    val direcciones = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val indice = ((grados + 22.5) / 45).toInt() % 8
    return direcciones[indice]
}

fun parsearClimaActual(json: String): ClimaActual? {
    return try {
        val current = JSONObject(json).getJSONObject("current")
        val temp = current.getDouble("temperature_2m")
        val sensacion = current.getDouble("apparent_temperature")
        val humedad = current.getInt("relative_humidity_2m")
        val viento = current.getDouble("wind_speed_10m")
        val codigo = current.getInt("weather_code")
        ClimaActual(
            temperatura = "${temp.toInt()}°",
            condicion = "${descripcionTiempo(codigo)} · sensación ${sensacion.toInt()}°",
            detalle = "Viento ${viento.toInt()} km/h " +
                "${direccionCompass(current.getInt("wind_direction_10m"))} · Humedad $humedad%"
        )
    } catch (_: Exception) {
        null
    }
}

fun pedirHttp(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    conn.setRequestProperty("User-Agent", "AlertaTemprana/1.0 (mseibelcastello@gmail.com)")
    val datos = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()
    return datos
}