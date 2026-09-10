package com.example.alertatemprana.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.alertatemprana.R
import com.example.alertatemprana.data.source.device.GrabadorAudio
import com.example.alertatemprana.data.source.device.GrabadorVideo
import com.example.alertatemprana.data.source.device.Linterna
import com.example.alertatemprana.data.source.firebase.ContactoEmergencia
import com.example.alertatemprana.data.source.firebase.ContactosRepository

@Composable
fun CommunicationsScreen() {
    val context = LocalContext.current
    val grabador = remember { GrabadorAudio(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val grabadorVideo = remember { GrabadorVideo(context, lifecycleOwner) }
    var pendienteSelfie: Boolean? = null

    LaunchedEffect(Unit) {
        grabadorVideo.iniciar()
    }

    var viewFinder: androidx.camera.view.PreviewView? = null
    var btnStart: Button? = null
    var btnStop: Button? = null
    var btnVideo: Button? = null
    var btnSelfie: Button? = null
    var btnVerUltima: Button? = null

    val empezarVideoGrabacion: (Boolean) -> Unit = { esSelfie ->
        viewFinder?.visibility = View.VISIBLE
        btnVideo?.isEnabled = false
        btnSelfie?.isEnabled = false
        btnStart?.isEnabled = false
        grabadorVideo.empezarGrabacion(esSelfie)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val todoOk = result.values.all { it }
        if (todoOk) {
            Toast.makeText(context, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            if (pendienteSelfie != null) {
                empezarVideoGrabacion(pendienteSelfie!!)
                pendienteSelfie = null
            }
        } else {
            Toast.makeText(context, "Se requieren permisos de cámara/Mic", Toast.LENGTH_SHORT)
                .show()
        }
    }

    val contactosState = remember { mutableStateOf<List<ContactoEmergencia>>(emptyList()) }
    val contactoPendienteState = remember { mutableStateOf<ContactoEmergencia?>(null) }
    val contactosRepository = remember { ContactosRepository() }

    fun llamarContacto(ctx: Context, contacto: ContactoEmergencia) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacto.telefono}"))
        try {
            ctx.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(ctx, "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendiente = contactoPendienteState.value
        contactoPendienteState.value = null
        if (granted && pendiente != null) {
            llamarContacto(context, pendiente)
        } else {
            Toast.makeText(context, "Permiso de llamadas requerido", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        contactosRepository.escuchar { lista ->
            contactosState.value = lista
        }
    }

    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.layout_communications, null)
            btnStart = view.findViewById(R.id.btnStart)
            btnStop = view.findViewById(R.id.btnStop)
            btnVideo = view.findViewById(R.id.btnVideo)
            btnSelfie = view.findViewById(R.id.btnSelfie)
            btnVerUltima = view.findViewById(R.id.btnVerUltima)
            viewFinder = view.findViewById(R.id.viewFinder)

            grabadorVideo.setPreviewView(viewFinder!!)
            grabadorVideo.onGrabando = {
                btnStop?.isEnabled = true
                Toast.makeText(ctx, "Grabando video...", Toast.LENGTH_SHORT).show()
                Unit
            }
            grabadorVideo.onVideoFinalizado = { ok, motivo ->
                viewFinder?.visibility = View.GONE
                if (ok) {
                    val nombre = grabadorVideo.ultimoVideoGrabado?.name ?: "video"
                    Toast.makeText(ctx, "Video guardado: $nombre", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(ctx, "No se pudo guardar: $motivo", Toast.LENGTH_LONG).show()
                }
                btnStart?.isEnabled = true
                btnStop?.isEnabled = false
                btnVideo?.isEnabled = true
                btnSelfie?.isEnabled = true
                Unit
            }

            btnStart?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    if (grabador.start()) {
                        btnStart?.isEnabled = false
                        btnStop?.isEnabled = true
                        Toast.makeText(ctx, "Grabando...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "No se pudo grabar", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            }

            btnStop?.setOnClickListener {
                if (btnVideo?.isEnabled == false) {
                    grabadorVideo.detenerGrabacion()
                } else {
                    grabador.stop()
                    btnStart?.isEnabled = true
                    btnStop?.isEnabled = false
                    Toast.makeText(ctx, "Audio guardado", Toast.LENGTH_LONG).show()
                }
            }

            btnVideo?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    empezarVideoGrabacion(false)
                } else {
                    pendienteSelfie = false
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                }
            }

            btnSelfie?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    empezarVideoGrabacion(true)
                } else {
                    pendienteSelfie = true
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                }
            }

            btnVerUltima?.setOnClickListener {
                val ultimo = grabadorVideo.ultimaGrabacion()
                if (ultimo == null) {
                    Toast.makeText(ctx, "No hay grabaciones todavía", Toast.LENGTH_SHORT).show()
                } else {
                    val uri = FileProvider.getUriForFile(
                        ctx,
                        "com.example.alertatemprana.fileprovider",
                        ultimo
                    )
                    val mime = if (grabadorVideo.esVideo(ultimo)) "video/mp4" else "audio/3gpp"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        ctx.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(ctx, "No hay reproductor disponible", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            view
        },
        update = { view ->
            val contenedor = view.findViewById<LinearLayout>(R.id.listaContactos)
            contenedor.removeAllViews()
            val lista = contactosState.value
            if (lista.isEmpty()) {
                val aviso = TextView(view.context).apply {
                    text = "No hay contactos de emergencia"
                    textSize = 14f
                }
                contenedor.addView(aviso)
            } else {
                lista.forEach { contacto ->
                    val fila = LinearLayout(view.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        val margin = (8 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = margin
                            bottomMargin = margin
                        }
                    }

                    val texto = TextView(view.context).apply {
                        textSize = 16f
                        text = "${contacto.nombre}\n${contacto.telefono}"
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val boton = Button(view.context).apply { text = "Llamar" }
                    boton.setOnClickListener {
                        if (ContextCompat.checkSelfPermission(
                                view.context, Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            llamarContacto(view.context, contacto)
                        } else {
                            contactoPendienteState.value = contacto
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }

                    fila.addView(texto)
                    fila.addView(boton)
                    contenedor.addView(fila)
                }
            }
        }
    )
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val device = remember { Linterna(context) }

    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.layout_home, null)
            val btnFlash = view.findViewById<Button>(R.id.btn_flash)
            btnFlash.setOnClickListener {
                device.toggle()
                btnFlash.text = if (device.isOn) "Apagar" else "Encender"
            }
            view
        }
    )
}

@Composable
fun PersonalScreen() {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_personal, null)
        }
    )
}