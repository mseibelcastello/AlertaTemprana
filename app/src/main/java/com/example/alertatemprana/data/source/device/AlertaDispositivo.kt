package com.example.alertatemprana.data.source.device

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.alertatemprana.R

class AlertaDispositivo(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator = getVibrator()

    private var reproductor: MediaPlayer? = null
    private var volumenPrevio = -1

    fun activar() {
        volumenPrevio = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)

        if (reproductor == null) {
            reproductor = MediaPlayer.create(
                appContext,
                R.raw.alerta,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                0
            )?.apply {
                isLooping = true
            }
        }
        reproductor?.start()

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 800, 400, 800, 400, 800),
                    0
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 800, 400, 800, 400, 800), 0)
        }
    }

    fun desactivar() {
        reproductor?.stop()
        reproductor?.release()
        reproductor = null
        vibrator.cancel()
        if (volumenPrevio >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumenPrevio, 0)
            volumenPrevio = -1
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= 31) {
            val manager = appContext
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}