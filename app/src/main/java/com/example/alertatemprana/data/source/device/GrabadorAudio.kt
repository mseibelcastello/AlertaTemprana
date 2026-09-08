package com.example.alertatemprana.data.source.device

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GrabadorAudio(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    fun start(): Boolean {
        return try {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(java.util.Date())
            val outputFile = File(musicDir, "AUDIO_$timeStamp.3gp")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(outputFile.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            false
        }
    }

    fun stop() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }
}