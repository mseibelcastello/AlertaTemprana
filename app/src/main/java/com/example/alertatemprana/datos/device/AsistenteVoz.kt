package com.example.alertatemprana.datos.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.alertatemprana.modelos.ComandoVoz

fun interpretarComando(texto: String): ComandoVoz {
    val t = texto.lowercase()
    val esLinterna = listOf("linterna", "flash", "luz").any { t.contains(it) }
    if (!esLinterna) return ComandoVoz.DESCONOCIDO
    return if (t.contains("apag")) {
        ComandoVoz.APAGAR_LINTERNA
    } else {
        ComandoVoz.ENCENDER_LINTERNA
    }
}

class AsistenteVoz(context: Context) {

    val disponible: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private val reconocedor: SpeechRecognizer? =
        if (disponible) SpeechRecognizer.createSpeechRecognizer(context) else null

    fun escuchar(
        onTexto: (String) -> Unit,
        onEstado: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val reco = reconocedor ?: return
        reco.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onEstado("Escuchando...")
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val mensaje = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH ->
                        "No te escuché. Probá de nuevo."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        "Reconocedor ocupado, probá de nuevo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Falta el permiso del micrófono."
                    else ->
                        "No se pudo reconocer la voz."
                }
                onError(mensaje)
            }

            override fun onResults(results: Bundle?) {
                val texto = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (texto.isNullOrBlank()) {
                    onError("No te escuché. Probá de nuevo.")
                } else {
                    onTexto(texto)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Decí un comando...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        reco.startListening(intent)
    }

    fun cerrar() {
        reconocedor?.destroy()
    }
}