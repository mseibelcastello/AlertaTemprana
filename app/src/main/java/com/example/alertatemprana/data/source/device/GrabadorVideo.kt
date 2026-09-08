package com.example.alertatemprana.data.source.device

import android.content.Context
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GrabadorVideo(
    context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var preview: Preview? = null
    private var pendienteEsSelfie = false

    var ultimoVideoGrabado: File? = null

    var onCamaraListo: (() -> Unit)? = null
    var onGrabando: (() -> Unit)? = null
    var onVideoFinalizado: ((Boolean, String) -> Unit)? = null

    fun setPreviewView(previewView: PreviewView) {
        val previewUseCase = Preview.Builder().build()
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        preview = previewUseCase
    }

    fun iniciar() {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            cameraProvider = future.get()
            onCamaraListo?.invoke()
            if (pendienteEsSelfie) {
                pendienteEsSelfie = false
                empezarGrabacion(true)
            }
        }, mainExecutor)
    }

    fun empezarGrabacion(esSelfie: Boolean) {
        if (cameraProvider == null) {
            pendienteEsSelfie = esSelfie
            return
        }
        val selector = if (esSelfie) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val nombre = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())

        val carpeta = File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "AlertaTemprana"
        ).apply { mkdirs() }
        val archivo = File(carpeta, "VIDEO_$nombre.mp4")

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
        videoCapture?.let { capture ->
            val provider = cameraProvider ?: return

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)

            val outputOptions = FileOutputOptions.Builder(archivo).build()

            recording = capture.output
                .prepareRecording(appContext, outputOptions)
                .withAudioEnabled()
                .start(mainExecutor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            onGrabando?.invoke()
                        }
                        is VideoRecordEvent.Finalize -> {
                            recording = null
                            if (event.hasError()) {
                                onVideoFinalizado?.invoke(
                                    false,
                                    event.error.toString()
                                )
                            } else {
                                ultimoVideoGrabado = archivo
                                onVideoFinalizado?.invoke(true, "")
                            }
                        }
                        else -> Unit
                    }
                }
        }
    }

    fun detenerGrabacion() {
        recording?.stop()
    }

    fun ultimaGrabacion(): File? {
        val audioDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val videoDir = File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "AlertaTemprana"
        )

        val audios = audioDir?.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith("AUDIO_") }
        val videos = videoDir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith("VIDEO_") }

        return (audios + videos).maxByOrNull { it.lastModified() }
    }

    fun esVideo(archivo: File): Boolean =
        archivo.extension.equals("mp4", ignoreCase = true)
}