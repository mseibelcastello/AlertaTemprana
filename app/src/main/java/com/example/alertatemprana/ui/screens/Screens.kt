package com.example.alertatemprana.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.alertatemprana.R
import com.example.alertatemprana.data.source.device.GrabadorAudio
import com.example.alertatemprana.data.source.device.GrabadorVideo
import com.example.alertatemprana.data.source.device.Linterna
import com.example.alertatemprana.data.source.device.Ubicacion
import com.example.alertatemprana.data.source.firebase.ContactoEmergencia
import com.example.alertatemprana.data.source.firebase.ContactosRepository

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

private data class ClimaActual(
    val temperatura: String,
    val condicion: String,
    val detalle: String
)

private fun parsearClimaActual(json: String): ClimaActual? {
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

private fun pedirHttp(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    val datos = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()
    return datos
}

private fun obtenerNombreLugar(lat: Double, lon: Double): String? {
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
    var btnGrabar: Button? = null

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

    val mostrarGrabacionState = remember { mutableStateOf(false) }

    if (mostrarGrabacionState.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.layout_grabacion, null)
            val btnVolverGrabacion = view.findViewById<Button>(R.id.btnVolverGrabacion)
            btnVolverGrabacion.setOnClickListener { mostrarGrabacionState.value = false }
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
        }
    )
        } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.layout_communications, null)
                btnGrabar = view.findViewById(R.id.btnGrabar)
                btnGrabar?.setOnClickListener {
                    mostrarGrabacionState.value = true
                }
                view
            }
        )
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val device = remember { Linterna(context) }
    val ubicacion = remember { Ubicacion(context) }
    val scope = rememberCoroutineScope()
    var webViewClima: WebView? = null
    var tvClimaTemp: TextView? = null
    var tvClimaCond: TextView? = null
    var tvClimaDet: TextView? = null
    val latDefecto = -35.6566
    val lonDefecto = -63.7575

    val contactosState = remember { mutableStateOf<List<ContactoEmergencia>>(emptyList()) }
    val contactoPendienteState = remember { mutableStateOf<ContactoEmergencia?>(null) }
    val contactosRepository = remember { ContactosRepository() }
    val mostrarContactosState = remember { mutableStateOf(false) }

    fun cargarClima(vista: WebView?, lat: Double, lon: Double) {
        vista?.loadUrl(
            "https://embed.windy.com/embed.html?type=forecast&location=coordinates" +
                "&detail=true&detailLat=$lat&detailLon=$lon" +
                "&metricTemp=default&metricRain=default&metricWind=default&embedMake=true"
        )
    }

    fun actualizarClimaActual(lat: Double, lon: Double, etiqueta: String) {
        scope.launch {
            val (clima, error) = withContext(Dispatchers.IO) {
                var error: String? = null
                var clima: ClimaActual? = null
                try {
                    clima = parsearClimaActual(
                        pedirHttp(
                            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                                "&current=temperature_2m,relative_humidity_2m,weather_code," +
                                "wind_speed_10m,wind_direction_10m,apparent_temperature&timezone=auto"
                        )
                    )
                } catch (e: Exception) {
                    error = e.message
                }
                clima to error
            }
            tvClimaTemp?.text = clima?.temperatura ?: "--"
            tvClimaCond?.text = if (clima != null) etiqueta else "Clima actual no disponible"
            tvClimaDet?.text = if (clima != null) {
                "${clima.condicion} · ${clima.detalle}"
            } else {
                error ?: ""
            }
        }
    }

    fun cargarClimaYUbicacion(lat: Double, lon: Double, usarGps: Boolean) {
        if (usarGps) {
            scope.launch {
                val etiqueta = withContext(Dispatchers.IO) {
                    obtenerNombreLugar(lat, lon)
                } ?: "Tu ubicación"
                actualizarClimaActual(lat, lon, etiqueta)
            }
        } else {
            actualizarClimaActual(lat, lon, "General Pico, La Pampa")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val loc = if (granted) ubicacion.obtenerUltima() else null
        val lat = loc?.first ?: latDefecto
        val lon = loc?.second ?: lonDefecto
        webViewClima?.post {
            cargarClima(webViewClima, lat, lon)
            cargarClimaYUbicacion(lat, lon, loc != null)
        }
    }

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

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun pintarContactos(vista: View, ctx: Context) {
        val contenedor = vista.findViewById<LinearLayout>(R.id.listaContactos) ?: return
        contenedor.removeAllViews()
        val lista = contactosState.value
        if (lista.isEmpty()) {
            val aviso = TextView(ctx).apply {
                text = "No hay contactos de emergencia"
                textSize = 14f
            }
            contenedor.addView(aviso)
        } else {
            lista.forEach { contacto ->
                val fila = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val margin = (8 * ctx.resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = margin
                        bottomMargin = margin
                    }
                }

                val texto = TextView(ctx).apply {
                    textSize = 16f
                    text = "${contacto.nombre}\n${contacto.telefono}"
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val boton = Button(ctx).apply { text = "Llamar" }
                boton.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        llamarContacto(ctx, contacto)
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

    if (mostrarContactosState.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.layout_contactos, null)
                val btnVolver = view.findViewById<Button>(R.id.btnVolverContactos)
                btnVolver.setOnClickListener { mostrarContactosState.value = false }
                pintarContactos(view, ctx)
                view
            },
            update = { view ->
                pintarContactos(view, view.context)
            }
        )
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.layout_home, null)
                val webView = view.findViewById<WebView>(R.id.webViewClima)
                webViewClima = webView
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.loadWithOverviewMode = true
                webView.settings.useWideViewPort = true
                webView.settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; SM-A505FN) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                webView.webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            view.loadDataWithBaseURL(
                                null,
                                "<html><body style='text-align:center;padding:32px;color:#333;background:#fff;font-family:sans-serif;'>" +
                                    "No se puede cargar el clima.<br>Revisá tu conexión a internet." +
                                    "</body></html>",
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                    }
                }
                tvClimaTemp = view.findViewById(R.id.tvClimaTemp)
                tvClimaCond = view.findViewById(R.id.tvClimaCond)
                tvClimaDet = view.findViewById(R.id.tvClimaDet)
                webView.setBackgroundColor(android.graphics.Color.WHITE)
                webView.post {
                    val gps = if (ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        ubicacion.obtenerUltima()
                    } else {
                        null
                    }
                    val lat = gps?.first ?: latDefecto
                    val lon = gps?.second ?: lonDefecto
                    cargarClima(webView, lat, lon)
                    cargarClimaYUbicacion(lat, lon, gps != null)
                }
                val btnFlash = view.findViewById<Button>(R.id.btn_flash)
                btnFlash.setOnClickListener {
                    device.toggle()
                    btnFlash.text = if (device.isOn) "Apagar" else "Encender"
                }
                val btnContactosHome = view.findViewById<Button>(R.id.btnContactosHome)
                btnContactosHome.setOnClickListener {
                    mostrarContactosState.value = true
                }
                view
            }
        )
    }
}

@Composable
fun PersonalScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_personal, null)
        }
    )
}