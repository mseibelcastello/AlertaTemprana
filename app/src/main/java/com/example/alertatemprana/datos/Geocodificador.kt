package com.example.alertatemprana.datos

import org.json.JSONObject

fun obtenerNombreLugar(lat: Double, lon: Double): String? {
    return try {
        val json = JSONObject(
            pedirHttp(
                "https://api.bigdatacloud.net/data/reverse-geocode-client?" +
                    "latitude=$lat&longitude=$lon&localityLanguage=es"
            )
        )
        val ciudad = json.optString("city").ifBlank { json.optString("locality") }
        val region = json.optString("principalSubdivision")
            .ifBlank { json.optString("countryName") }
        when {
            ciudad.isNotBlank() && region.isNotBlank() -> "$ciudad, $region"
            ciudad.isNotBlank() -> ciudad
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun obtenerDireccionExacta(lat: Double, lon: Double): String {
    return try {
        val json = JSONObject(
            pedirHttp(
                "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon" +
                    "&format=jsonv2&addressdetails=1&accept-language=es&zoom=18"
            )
        )
        val a = json.optJSONObject("address")
        if (a == null) return ""
        val calle = a.optString("house_number") to a.optString("road")
        val partes = mutableListOf<String>()
        listOf(calle.second, calle.first)
            .filter { it.isNotBlank() }.joinToString(" ").trim().let {
                if (it.isNotBlank()) partes += it
            }
        listOf(
            a.optString("suburb"),
            a.optString("town").ifBlank { a.optString("city") },
            a.optString("state"),
            a.optString("postcode")
        ).filter { it.isNotBlank() && it !in partes }.forEach { partes += it }
        partes.joinToString(", ")
    } catch (_: Exception) {
        ""
    }
}

fun obtenerDireccion(lat: Double, lon: Double): String {
    return try {
        val json = JSONObject(
            pedirHttp(
                "https://api.bigdatacloud.net/data/reverse-geocode-client?" +
                    "latitude=$lat&longitude=$lon&localityLanguage=es&addressdetails=1"
            )
        )
        val calle = listOf(json.optString("street"), json.optString("houseNumber"))
            .filter { it.isNotBlank() }.joinToString(" ").trim()
        val ciudad = json.optString("locality").ifBlank { json.optString("city") }
        val region = json.optString("principalSubdivision")
            .ifBlank { json.optString("countryName") }
        listOf(calle, ciudad, region).filter { it.isNotBlank() }.joinToString(", ")
    } catch (_: Exception) {
        ""
    }
}

fun obtenerDireccionFoton(lat: Double, lon: Double): String {
    return try {
        val json = JSONObject(pedirHttp("https://photon.komoot.io/reverse?lat=$lat&lon=$lon&lang=es"))
        val prop = json.optJSONArray("features")
            ?.optJSONObject(0)?.optJSONObject("properties") ?: return ""
        listOf(
            prop.optString("street"),
            prop.optString("housenumber"),
            prop.optString("city").ifBlank { prop.optString("town") },
            prop.optString("state"),
            prop.optString("country")
        ).filter { it.isNotBlank() }.joinToString(", ")
    } catch (_: Exception) {
        ""
    }
}

fun buscarDireccion(lat: Double, lon: Double): String {
    val exacta = obtenerDireccionExacta(lat, lon)
    if (exacta.isNotBlank()) return exacta
    val generica = obtenerDireccion(lat, lon)
    if (generica.isNotBlank()) return generica
    return obtenerDireccionFoton(lat, lon)
}