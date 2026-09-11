package com.example.alertatemprana.datos.device

import android.content.Context
import android.os.BatteryManager
import kotlin.math.abs
import kotlin.math.roundToInt

class Bateria(context: Context) {

    private val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val consumoMinimoMa = 250.0

    fun resumen(): String {
        val nivel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val nivelReal = if (nivel > 0) nivel else 50

        val contador = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val mAhRestantes = if (contador != Int.MIN_VALUE.toLong() && contador > 0) {
            contador / 1000.0
        } else {
            nivelReal * 40.0
        }

        var corriente = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        if (corriente == Int.MIN_VALUE.toLong()) {
            corriente = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }

        val consumoMa = if (corriente != Int.MIN_VALUE.toLong()) {
            maxOf(abs(corriente).toDouble() / 1000.0, consumoMinimoMa)
        } else {
            consumoMinimoMa
        }

        return formatear(mAhRestantes / consumoMa)
    }

    private fun formatear(horas: Double): String {
        return if (horas >= 24.0) {
            "más de 24 h"
        } else {
            val h = horas.toInt()
            val m = ((horas - h) * 60).roundToInt()
            if (h > 0) "~$h h $m m" else "~$m min"
        }
    }
}