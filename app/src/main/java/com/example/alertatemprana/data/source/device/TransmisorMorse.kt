package com.example.alertatemprana.data.source.device

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TransmisorMorse(context: Context) {

    private val appContext = context.applicationContext
    val flashDisponible: Boolean = tieneFlash(appContext)
    private val linterna: Linterna? =
        if (flashDisponible) Linterna(appContext) else null

    private var toneGenerator: ToneGenerator? = null
    private var job: Job? = null

    val estaTransmitiendo: Boolean get() = job?.isActive == true

    fun transmitirConLinterna(
        mensaje: String,
        scope: CoroutineScope,
        onInicio: () -> Unit = {},
        onFin: () -> Unit = {}
    ) {
        if (!flashDisponible || estaTransmitiendo) return
        val lin = linterna ?: return
        job = scope.launch {
            onInicio()
            try {
                for ((duracion, encendido) in generarSecuencia(textoAMorse(mensaje))) {
                    if (encendido && !lin.isOn) lin.toggle()
                    delay(duracion)
                    if (lin.isOn) lin.toggle()
                }
            } finally {
                if (lin.isOn) lin.toggle()
                onFin()
            }
        }
    }

    fun transmitirConSonido(
        mensaje: String,
        scope: CoroutineScope,
        onInicio: () -> Unit = {},
        onFin: () -> Unit = {}
    ) {
        if (estaTransmitiendo) return
        job = scope.launch {
            onInicio()
            try {
                val generador = obtenerToneGenerator()
                for ((duracion, encendido) in generarSecuencia(textoAMorse(mensaje))) {
                    if (encendido) {
                        generador.startTone(ToneGenerator.TONE_SUP_RINGTONE, duracion.toInt())
                    }
                    delay(duracion)
                }
            } finally {
                toneGenerator?.stopTone()
                onFin()
            }
        }
    }

    fun cerrar() {
        job?.cancel()
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun obtenerToneGenerator(): ToneGenerator {
        return toneGenerator ?: ToneGenerator(
            AudioManager.STREAM_ALARM,
            80
        ).also { toneGenerator = it }
    }

    private fun tieneFlash(contexto: Context): Boolean {
        return try {
            val manager = contexto
                .getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.cameraIdList.any { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            false
        }
    }
}