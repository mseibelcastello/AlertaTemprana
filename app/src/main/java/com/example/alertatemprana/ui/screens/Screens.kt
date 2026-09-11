package com.example.alertatemprana.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.util.MapTileIndex
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.alertatemprana.R
import com.example.alertatemprana.data.source.device.AsistenteVoz
import com.example.alertatemprana.data.source.device.Bateria
import com.example.alertatemprana.data.source.device.ComandoVoz
import com.example.alertatemprana.data.source.device.GrabadorAudio
import com.example.alertatemprana.data.source.device.GrabadorVideo
import com.example.alertatemprana.data.source.device.Linterna
import com.example.alertatemprana.data.source.device.TransmisorMorse
import com.example.alertatemprana.data.source.device.Ubicacion
import com.example.alertatemprana.data.source.device.interpretarComando
import com.example.alertatemprana.data.source.device.textoAMorse
import com.example.alertatemprana.data.source.firebase.AlertaCatastrofe
import com.example.alertatemprana.data.source.firebase.ChatRepository
import com.example.alertatemprana.data.source.firebase.ContactoEmergencia
import com.example.alertatemprana.data.source.firebase.ContactosRepository
import com.example.alertatemprana.data.source.firebase.MensajeChat
import com.example.alertatemprana.data.source.firebase.RegistroUbicacion
import com.example.alertatemprana.data.source.firebase.UbicacionesRepository
import com.example.alertatemprana.ui.components.BotonApp
import com.example.alertatemprana.ui.components.TileApp
import com.example.alertatemprana.ui.theme.Emergencia
import com.example.alertatemprana.ui.theme.Fondo
import com.example.alertatemprana.ui.theme.Primario
import com.example.alertatemprana.ui.theme.PrimarioClaro
import com.example.alertatemprana.ui.theme.Secundario
import com.example.alertatemprana.ui.theme.Seguro
import com.example.alertatemprana.ui.theme.Superficie
import com.example.alertatemprana.ui.theme.SuperficieVariante
import com.example.alertatemprana.ui.theme.TextoPrincipal
import com.example.alertatemprana.ui.theme.TextoSecundario

private val esriTileSource = object : OnlineTileSourceBase(
    "EsriWorldStreetMap", 0, 19, 256, ".png",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    "Powered by Esri"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + ".png"
    }
}

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
    conn.setRequestProperty("User-Agent", "AlertaTemprana/1.0 (mseibelcastello@gmail.com)")
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

private fun obtenerDireccionExacta(lat: Double, lon: Double): String {
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

private fun obtenerDireccion(lat: Double, lon: Double): String {
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

private fun obtenerDireccionFoton(lat: Double, lon: Double): String {
    return try {
        val json = JSONObject(
            pedirHttp("https://photon.komoot.io/reverse?lat=$lat&lon=$lon&lang=es")
        )
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

private fun buscarDireccion(lat: Double, lon: Double): String {
    val exacta = obtenerDireccionExacta(lat, lon)
    if (exacta.isNotBlank()) return exacta
    val generica = obtenerDireccion(lat, lon)
    if (generica.isNotBlank()) return generica
    return obtenerDireccionFoton(lat, lon)
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
    val mostrarChat = remember { mutableStateOf(false) }
    val mostrarMorse = remember { mutableStateOf(false) }

    BackHandler(enabled = mostrarChat.value || mostrarMorse.value) {
        if (mostrarMorse.value) {
            mostrarMorse.value = false
        } else {
            mostrarChat.value = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Primario)
                        )
                        Text(
                            "COMUNICACIONES",
                            color = TextoPrincipal,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            "Grabar · Chat · Morse",
                            color = TextoSecundario,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

            Spacer(modifier = Modifier.height(12.dp))

            TileApp(
                etiqueta = "Morse",
                dato = "SOS",
                detalle = "Transmití en código Morse",
                colorFondo = Primario,
                onClick = { mostrarMorse.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 118.dp)
            )
            TileApp(
                etiqueta = "Chat",
                dato = "Asistencia",
                detalle = "Mensajes de emergencia en vivo",
                colorFondo = Secundario,
                onClick = { mostrarChat.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 118.dp)
            )
            TileApp(
                etiqueta = "Grabación",
                dato = "Audio / Video",
                detalle = "Crear un nuevo registro",
                colorFondo = Superficie,
                colorTexto = TextoPrincipal,
                detalleColor = TextoSecundario,
                onClick = { mostrarGrabacionState.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 118.dp)
            )
        }
    }

        if (mostrarChat.value) {
            ChatScreen(onCerrar = { mostrarChat.value = false })
        }

        if (mostrarMorse.value) {
            TraductorMorseScreen(onCerrar = { mostrarMorse.value = false })
        }
    }
}

@Composable
fun HomeScreen(
    alerta: AlertaCatastrofe?,
    onIrAComunicaciones: () -> Unit,
    onIrAPersonal: () -> Unit
) {
    val context = LocalContext.current
    val device = remember { Linterna(context) }
    val bateria = remember { Bateria(context) }
    val ubicacion = remember { Ubicacion(context) }
    val scope = rememberCoroutineScope()
    var webViewClima: WebView? = null

    val clima = remember { mutableStateOf<ClimaActual?>(null) }
    val climaEtiqueta = remember { mutableStateOf("General Pico, La Pampa") }
    val bateriaEstado = remember { mutableStateOf(bateria.resumen()) }
    val vozEstado = remember {
        mutableStateOf("Tocá el micrófono y decí \"prende la linterna\"")
    }
    val linterna = remember { mutableStateOf(false) }

    val latDefecto = -35.6566
    val lonDefecto = -63.7575

    val contactosState = remember { mutableStateOf<List<ContactoEmergencia>>(emptyList()) }
    val contactosError = remember { mutableStateOf<String?>(null) }
    val contactoPendienteState = remember { mutableStateOf<ContactoEmergencia?>(null) }
    val contactosRepository = remember { ContactosRepository() }
    val mostrarGuia = remember { mutableStateOf(false) }
    val guiaSeleccionada = remember { mutableStateOf<String?>(null) }
    val mostrarEmergencias = remember { mutableStateOf(false) }
    val mostrarMorseHome = remember { mutableStateOf(false) }

    val asistenteVoz = remember { AsistenteVoz(context) }

    val escucharVoz: () -> Unit = {
        if (asistenteVoz.disponible) {
            asistenteVoz.escuchar(
            onTexto = { texto ->
                when (interpretarComando(texto)) {
                    ComandoVoz.ENCENDER_LINTERNA -> {
                        if (!device.isOn) device.toggle()
                        linterna.value = device.isOn
                        vozEstado.value = "Comando: \"$texto\" → linterna encendida"
                        Toast.makeText(context, "Linterna encendida", Toast.LENGTH_SHORT).show()
                    }
                    ComandoVoz.APAGAR_LINTERNA -> {
                        device.turnOff()
                        linterna.value = device.isOn
                        vozEstado.value = "Comando: \"$texto\" → linterna apagada"
                        Toast.makeText(context, "Linterna apagada", Toast.LENGTH_SHORT).show()
                    }
                    ComandoVoz.DESCONOCIDO -> {
                        vozEstado.value = "No entendí \"$texto\""
                        Toast.makeText(context, "No entendí el comando", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onEstado = { msg -> vozEstado.value = msg },
            onError = { msg ->
                vozEstado.value = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
        } else {
            Toast.makeText(
                context,
                "Reconocimiento de voz no disponible en este dispositivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val launcherVoz = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            escucharVoz()
        } else {
            Toast.makeText(
                context,
                "Se requiere el micrófono para el comando de voz",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            asistenteVoz.cerrar()
        }
    }

    BackHandler(enabled = mostrarGuia.value) {
        if (guiaSeleccionada.value != null) {
            guiaSeleccionada.value = null
        } else {
            mostrarGuia.value = false
        }
    }

    fun cargarClima(vista: WebView?, lat: Double, lon: Double) {
        vista?.loadUrl(
            "https://embed.windy.com/embed.html?type=forecast&location=coordinates" +
                "&detail=true&detailLat=$lat&detailLon=$lon" +
                "&metricTemp=default&metricRain=default&metricWind=default&embedMake=true"
        )
    }

    fun actualizarClimaActual(lat: Double, lon: Double, etiqueta: String) {
        scope.launch {
            val (datos, error) = withContext(Dispatchers.IO) {
                var error: String? = null
                var datos: ClimaActual? = null
                try {
                    datos = parsearClimaActual(
                        pedirHttp(
                            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                                "&current=temperature_2m,relative_humidity_2m,weather_code," +
                                "wind_speed_10m,wind_direction_10m,apparent_temperature&timezone=auto"
                        )
                    )
                } catch (e: Exception) {
                    error = e.message
                }
                datos to error
            }
            if (datos != null) {
                clima.value = datos
                climaEtiqueta.value = etiqueta
            } else {
                clima.value = null
                climaEtiqueta.value = error ?: "Clima actual no disponible"
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
        contactosRepository.escuchar(
            onContactos = { lista ->
                contactosState.value = lista
                contactosError.value = null
            },
            onError = { e ->
                contactosError.value = e.message ?: "Error desconocido"
            }
        )
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            bateriaEstado.value = bateria.resumen()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            mostrarGuia.value -> {
                if (guiaSeleccionada.value != null) {
                    GuiaDetalleScreen(
                        catastrofe = guiaSeleccionada.value!!,
                        onCerrar = { guiaSeleccionada.value = null }
                    )
                } else {
                    GuiaScreen(
                        onSeleccionar = { guiaSeleccionada.value = it },
                        onCerrar = { mostrarGuia.value = false }
                    )
                }
            }
            mostrarEmergencias.value -> {
                EmergenciasScreen(
                    lista = contactosState.value,
                    error = contactosError.value,
                    onLlamar = { contacto ->
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            llamarContacto(context, contacto)
                        } else {
                            contactoPendienteState.value = contacto
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                    onCerrar = { mostrarEmergencias.value = false }
                )
            }
            mostrarMorseHome.value -> {
                TraductorMorseScreen(onCerrar = { mostrarMorseHome.value = false })
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Primario)
                        )
                        Text(
                            "ALERTA TEMPRANA",
                            color = TextoPrincipal,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            "Ante catástrofes naturales",
                            color = TextoSecundario,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val hayAlerta = alerta != null
                    TileApp(
                        etiqueta = "Estado de alerta",
                        dato = if (hayAlerta) "ALERTA ACTIVA" else "SIN ALERTAS",
                        detalle = alerta?.tipo?.uppercase() ?: "Todo en orden",
                        colorFondo = if (hayAlerta) Emergencia else Seguro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TileApp(
                            etiqueta = "Morse",
                            dato = "SOS",
                            detalle = "··· --- ···",
                            colorFondo = Primario,
                            onClick = { mostrarMorseHome.value = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TileApp(
                            etiqueta = "Mi ubicación",
                            dato = "Mapa",
                            detalle = "Ver en vivo",
                            colorFondo = Secundario,
                            onClick = onIrAPersonal,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                    }

                    TileApp(
                        etiqueta = "Clima",
                        dato = clima.value?.temperatura ?: "--",
                        detalle = clima.value?.let {
                            "${climaEtiqueta.value} · ${it.condicion}"
                        } ?: climaEtiqueta.value,
                        colorFondo = Superficie,
                        colorTexto = TextoPrincipal,
                        detalleColor = TextoSecundario,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 112.dp)
                    )

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Mapa en vivo",
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextoSecundario
                        )
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).also { web ->
                                    webViewClima = web
                                    web.settings.javaScriptEnabled = true
                                    web.settings.domStorageEnabled = true
                                    web.settings.loadWithOverviewMode = true
                                    web.settings.useWideViewPort = true
                                    web.settings.userAgentString =
                                        "Mozilla/5.0 (Linux; Android 10; SM-A505FN) AppleWebKit/537.36 " +
                                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                    web.webViewClient = object : WebViewClient() {
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
                                    web.setBackgroundColor(android.graphics.Color.WHITE)
                                    web.post {
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
                                        cargarClima(web, lat, lon)
                                        cargarClimaYUbicacion(lat, lon, gps != null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .height(200.dp)
                        )
                    }

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TileApp(
                            etiqueta = "Emergencias",
                            dato = if (contactosState.value.isEmpty()) {
                                "SIN NÚMEROS"
                            } else {
                                contactosState.value.joinToString(" · ") { it.telefono }
                            },
                            datoTamanio = 18.sp,
                            detalle = if (contactosState.value.isEmpty()) {
                                "Tocá para cargar"
                            } else {
                                "Tocá para llamar"
                            },
                            colorFondo = Primario,
                            onClick = { mostrarEmergencias.value = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TileApp(
                            etiqueta = "Comunicación",
                            dato = "Comunicar",
                            detalle = "Grabar · Chat · Morse",
                            colorFondo = Secundario,
                            onClick = onIrAComunicaciones,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                    }

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TileApp(
                            etiqueta = "Voz",
                            dato = "Asistente",
                            detalle = vozEstado.value,
                            colorFondo = Superficie,
                            colorTexto = TextoPrincipal,
                            detalleColor = TextoSecundario,
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    escucharVoz()
                                } else {
                                    launcherVoz.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TileApp(
                            etiqueta = "Linterna",
                            dato = if (linterna.value) "Apagar" else "Encender",
                            colorFondo = PrimarioClaro,
                            colorTexto = TextoPrincipal,
                            onClick = {
                                device.toggle()
                                linterna.value = device.isOn
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp)
                        )
                    }

                    TileApp(
                        etiqueta = "Batería",
                        dato = bateriaEstado.value,
                        detalle = "Estado del equipo",
                        colorFondo = Superficie,
                        colorTexto = TextoPrincipal,
                        detalleColor = TextoSecundario,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 104.dp)
                    )
                }
            }
        }
    }
}

data class GuiaCatastrofe(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val imagen: Int,
    val videoUrl: String,
    val pasos: List<String>
)

private val guias = listOf(
    GuiaCatastrofe(
        id = "terremoto",
        titulo = "Terremoto",
        descripcion = "Un sismo o terremoto es un movimiento brusco de la tierra, causado por la liberación repentina de energía dentro de la misma tierra. En Argentina se producen por el contacto de la placa de Nazca con la placa Sudamericana, sobre todo en las provincias del oeste, aunque ninguna parte del país está exenta de este fenómeno.",
        imagen = R.drawable.terremoto,
        videoUrl = "https://www.youtube.com/embed/UUGT39JINoI",
        pasos = listOf(
            "Mantené la calma y ubicate en un lugar seguro, debajo de un elemento firme y si no es posible, junto a él.",
            "Cortá la energía eléctrica y cerrá las llaves de paso de agua y gas.",
            "Para iluminar, usá linternas. Velas, fósforos o encendedores pueden provocar explosiones en caso de fuga de gas.",
            "En la calle, mantenete alejado de edificios, postes y cables eléctricos.",
            "Si estás conduciendo, disminuí la velocidad. En lo posible, detenete en un lugar seguro.",
            "Protegéte la cabeza y el cuello con los brazos y esperá instrucciones de las autoridades.",
            "Si quedaste encerrado/a o atrapado/a, mantené la calma y solicitá auxilio."
        )
    ),
    GuiaCatastrofe(
        id = "inundacion",
        titulo = "Inundación",
        descripcion = "Una inundación se produce cuando hay un rápido crecimiento del nivel del agua que cubre o llena determinadas áreas. En la Argentina se producen por lluvias intensas, fuertes vientos, deshielo, desborde de represas o actividades humanas como la tala de árboles o la impermeabilización de suelos.",
        imagen = R.drawable.inundacion,
        videoUrl = "https://www.youtube.com/embed/0toqpF7tFN4",
        pasos = listOf(
            "Conservá la calma. Cerrá la llave de gas, agua y cortá la electricidad.",
            "Evacuá hacia una zona segura, que por lo general son las más altas.",
            "Si te sorprende el agua dentro de la vivienda, evitá sótanos y planta baja. Llamá a los Bomberos para que te ayuden a evacuar.",
            "Mantené cerradas puertas y ventanas, para evitar corrientes de agua dentro de la vivienda.",
            "No usés ningún tipo de vehículo como auto, moto o bicicleta.",
            "No te ubiques cerca de postes de electricidad o cables.",
            "Si hay heridos, llamá a las autoridades. No intentés moverlos.",
            "No intentés caminar o nadar por caminos inundados o cauces de río.",
            "Tomá agua potabilizada, no utilicés el agua de la canilla."
        )
    ),
    GuiaCatastrofe(
        id = "incendio",
        titulo = "Incendio forestal",
        descripcion = "Un incendio forestal es un fuego descontrolado de rápida propagación que afecta a bosques, llanuras, pastizales y pasturas, entre otras. El 95% de los incendios forestales son producidos por la mano del hombre: fogatas y colillas mal apagadas, abandono de tierras o preparación de áreas de pastoreo con fuego.",
        imagen = R.drawable.incendio_forestal,
        videoUrl = "https://www.youtube.com/embed/7M_7DP6EQJw",
        pasos = listOf(
            "Mantené las puertas y ventanas totalmente cerradas para evitar el ingreso del humo y de las chispas.",
            "Tratá que el suelo alrededor de la vivienda esté húmedo para evitar el avance del fuego.",
            "No salgas de tu casa a menos que el personal de bomberos o de las Fuerzas de Seguridad te lo indique, o que el riesgo de incendio de la vivienda sea inminente.",
            "Si la autoridad determina la evacuación, acatá las indicaciones. Procurá cubrirte boca y nariz con un paño, para no inhalar humo.",
            "No vuelvas a un área quemada. Los sitios calientes pueden reactivarse sin previo aviso.",
            "Nunca te sitúes en la parte alta de una montaña ni corras en sentido ascendente: el fuego avanza al subir hasta 17 veces más rápido que tú.",
            "Si la situación se torna peligrosa, acostate en el suelo y tratá de respirar a través de una prenda mojada.",
            "Si estás en una ruta y ves una columna de humo, avisá de inmediato a los bomberos."
        )
    )
)

@Composable
fun GuiaScreen(
    onSeleccionar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonApp(
                texto = "Volver",
                icono = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = onCerrar
            )
            Text(
                "Guía de acción",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        Text(
            "Seleccioná una catástrofe para ver cómo actuar",
            fontSize = 14.sp,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(guias) { g ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Superficie, RoundedCornerShape(16.dp))
                        .clickable { onSeleccionar(g.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        g.titulo,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("→", fontSize = 18.sp, color = TextoSecundario)
                }
            }
        }
    }
}

@Composable
fun GuiaDetalleScreen(catastrofe: String, onCerrar: () -> Unit) {
    val g = guias.firstOrNull { it.id == catastrofe } ?: return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.layout_guia_detalle, null)
            val btnVolver = view.findViewById<Button>(R.id.btnVolverGuia)
            btnVolver.setOnClickListener { onCerrar() }
            val tvTitulo = view.findViewById<TextView>(R.id.tvTituloGuia)
            tvTitulo.text = g.titulo
            val tvDesc = view.findViewById<TextView>(R.id.tvGuiaDescripcion)
            tvDesc.text = g.descripcion
            val img = view.findViewById<android.widget.ImageView>(R.id.imgGuia)
            img.setImageResource(g.imagen)
            val web = view.findViewById<WebView>(R.id.webVideoGuia)
            val videoId = g.videoUrl.substringAfterLast("/")
            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.loadWithOverviewMode = true
            web.settings.useWideViewPort = true
            web.settings.mediaPlaybackRequiresUserGesture = false
            web.settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; SM-A505FN) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            web.webChromeClient = WebChromeClient()
            web.loadUrl("https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1")
            val contenedor = view.findViewById<LinearLayout>(R.id.listaPasosGuia)
            g.pasos.forEach { paso ->
                val tv = TextView(ctx).apply {
                    text = "• $paso"
                    textSize = 14f
                    setPadding(0, 6, 0, 6)
                }
                contenedor.addView(tv)
            }
            view
        }
    )
}

@Composable
fun PersonalScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ubicacion = remember { Ubicacion(context) }
    val repo = remember { UbicacionesRepository() }
    val permisoConcedido = remember { mutableStateOf(false) }
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val marcador = remember { mutableStateOf<Marker?>(null) }
    val primeraPosicion = remember { mutableStateOf(true) }
    val tvDireccion = remember { mutableStateOf<TextView?>(null) }
    val ultimaPos = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val ultimoGeo = remember { mutableStateOf(0L) }
    val verUltimas = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = verUltimas.value) {
        verUltimas.value = false
    }

    fun actualizarPosicion(lat: Double, lon: Double) {
        ultimaPos.value = lat to lon
        val mapa = mapView.value ?: return
        val punto = GeoPoint(lat, lon)
        val mark = marcador.value ?: Marker(mapa).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            it.title = "Mi ubicación"
            mapa.overlays.add(it)
            marcador.value = it
        }
        mark.position = punto
        if (primeraPosicion.value) {
            mapa.controller.setZoom(18.0)
            primeraPosicion.value = false
        }
        mapa.controller.setCenter(punto)
        mapa.invalidate()

        val ahora = System.currentTimeMillis()
        if (ahora - ultimoGeo.value >= 30_000) {
            ultimoGeo.value = ahora
            scope.launch(Dispatchers.IO) {
                val dir = buscarDireccion(lat, lon)
                withContext(Dispatchers.Main) {
                    tvDireccion.value?.text = dir.ifBlank { "Dirección no disponible" }
                }
            }
        }
    }

    fun iniciarSeguimiento() {
        if (!permisoConcedido.value) return
        ubicacion.iniciarSeguimiento { location ->
            actualizarPosicion(location.latitude, location.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permisoConcedido.value = granted
        if (granted) {
            ubicacion.obtenerUltima()?.let { actualizarPosicion(it.first, it.second) }
            iniciarSeguimiento()
        }
    }

    LaunchedEffect(Unit) {
        permisoConcedido.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!permisoConcedido.value) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            iniciarSeguimiento()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.value?.onResume()
                    iniciarSeguimiento()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    ubicacion.detenerSeguimiento()
                    mapView.value?.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ubicacion.detenerSeguimiento()
            mapView.value?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vista = LayoutInflater.from(ctx).inflate(R.layout.layout_personal, null)
            val mapa = vista.findViewById<MapView>(R.id.mapPersonal)
            (mapa.layoutParams as LinearLayout.LayoutParams).let {
                it.height = ctx.resources.displayMetrics.heightPixels / 2
                it.weight = 0f
                mapa.layoutParams = it
            }
            mapa.setTileSource(esriTileSource)
            mapa.setMultiTouchControls(true)
            mapa.controller.setZoom(15.0)
            mapa.controller.setCenter(GeoPoint(-35.6566, -63.7575))

            if (ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                ubicacion.obtenerUltima()?.let {
                    mapa.controller.setZoom(18.0)
                    mapa.controller.setCenter(GeoPoint(it.first, it.second))
                    marcador.value = Marker(mapa).also { m ->
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        m.title = "Mi ubicación"
                            m.position = GeoPoint(it.first, it.second)
                            mapa.overlays.add(m)
                    }
                    primeraPosicion.value = false
                }
            }

            tvDireccion.value = vista.findViewById(R.id.tvDireccion)
            vista.findViewById<Button>(R.id.btnRegistrarUbicacion).setOnClickListener { btn ->
                val pos = ultimaPos.value ?: ubicacion.obtenerUltima()
                if (pos == null) {
                    Toast.makeText(
                        ctx, "Todavía no hay ubicación disponible", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                btn.isEnabled = false
                val direccionVisible = tvDireccion.value?.text?.toString() ?: ""
                scope.launch(Dispatchers.IO) {
                    val dir = obtenerDireccionExacta(pos.first, pos.second)
                        .ifBlank { direccionVisible }
                    withContext(Dispatchers.Main) {
                        repo.guardar(
                            latitud = pos.first,
                            longitud = pos.second,
                            direccion = dir,
                            onOk = {
                                Toast.makeText(
                                    ctx, "Ubicación registrada", Toast.LENGTH_SHORT
                                ).show()
                                btn.isEnabled = true
                            },
                            onError = { err ->
                                Toast.makeText(
                                    ctx, "Error: $err", Toast.LENGTH_SHORT
                                ).show()
                                btn.isEnabled = true
                            }
                        )
                    }
                }
            }
            vista.findViewById<Button>(R.id.btnVerUltimas).setOnClickListener {
                verUltimas.value = true
            }
            mapView.value = mapa
            vista
        }
        )

        if (verUltimas.value) {
            UltimasUbicacionesScreen(
                repositorio = repo,
                onCerrar = { verUltimas.value = false }
            )
        }
    }
}

@Composable
fun UltimasUbicacionesScreen(
    repositorio: UbicacionesRepository,
    onCerrar: () -> Unit
) {
    val registros = remember { mutableStateOf<List<RegistroUbicacion>>(emptyList()) }
    val error = remember { mutableStateOf<String?>(null) }
    val cargando = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repositorio.leerUltimas(
            limite = 20,
            onListo = { lista ->
                registros.value = lista
                cargando.value = false
            },
            onError = { msg ->
                error.value = msg
                cargando.value = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonApp(
                texto = "Volver",
                icono = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = onCerrar
            )
            Text(
                "Últimos registros",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            when {
                cargando.value -> Text("Cargando...")
                error.value != null -> Text("Error: ${error.value}")
                registros.value.isEmpty() -> Text("Sin registros todavía")
                else -> {
                    val fmt = remember {
                        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    }
                    LazyColumn {
                        items(registros.value) { r ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    r.direccion.ifBlank { "Sin dirección" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    r.fecha?.let { fmt.format(it) } ?: "Fecha no disponible",
                                    fontSize = 13.sp,
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergenciasScreen(
    lista: List<ContactoEmergencia>,
    error: String?,
    onLlamar: (ContactoEmergencia) -> Unit,
    onCerrar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonApp(
                    texto = "Volver",
                    icono = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onCerrar
                )
                Text(
                    text = "EMERGENCIAS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Text(
                text = "Números de emergencia",
                color = TextoSecundario,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (error != null) {
                Text(
                    text = "No se pudieron cargar (¿reglas?): $error",
                    color = Emergencia,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (lista.isEmpty() && error == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay números de emergencia",
                        color = TextoSecundario,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Agregalos en Firestore → numeros_emergencia",
                        color = TextoSecundario.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                lista.forEach { contacto ->
                    TileApp(
                        etiqueta = contacto.nombre,
                        dato = contacto.telefono,
                        datoTamanio = 36.sp,
                        colorFondo = Primario,
                        onClick = { onLlamar(contacto) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 96.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(onCerrar: () -> Unit) {
    val repo = remember { ChatRepository() }
    val mensajes = remember { mutableStateOf<List<MensajeChat>>(emptyList()) }
    val entrada = remember { mutableStateOf("") }
    val errorConexion = remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = repo.escuchar { m ->
            val lista = mensajes.value.toMutableList()
            lista.add(m)
            mensajes.value = lista
        }
        onDispose { repo.detener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonApp(
                texto = "Volver",
                icono = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = onCerrar
            )
            Text(
                "Chat de asistencia",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        errorConexion.value?.let {
            Text("Error: $it", color = Emergencia, fontSize = 13.sp)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(mensajes.value) { m ->
                val esUsuario = m.emisor == "usuario"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .widthIn(max = 280.dp)
                            .background(
                                color = if (esUsuario) Primario else SuperficieVariante,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            m.texto,
                            color = if (esUsuario) androidx.compose.ui.graphics.Color.White else TextoPrincipal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = entrada.value,
                onValueChange = { entrada.value = it },
                placeholder = { Text("Escribí un mensaje...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            BotonApp(
                texto = "Enviar",
                icono = Icons.AutoMirrored.Outlined.Send,
                onClick = {
                    val texto = entrada.value.trim()
                    if (texto.isEmpty()) return@BotonApp
                    entrada.value = ""
                    repo.enviar(
                        texto, "usuario",
                        onOk = {},
                        onError = { errorConexion.value = it }
                    )
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun TraductorMorseScreen(onCerrar: () -> Unit) {
    val context = LocalContext.current
    val transmisor = remember { TransmisorMorse(context) }
    val scope = rememberCoroutineScope()
    val transmitiendo = remember { mutableStateOf(false) }
    val palabras = listOf("SOS", "AYUDA", "PELIGRO", "SALIR")
    val palabraSeleccionada = remember { mutableStateOf("SOS") }
    var permisoPendiente: String? = null

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            permisoPendiente?.let { palabra ->
                transmisor.transmitirConLinterna(
                    palabra, scope,
                    onInicio = { transmitiendo.value = true },
                    onFin = { transmitiendo.value = false }
                )
                permisoPendiente = null
            }
        } else {
            Toast.makeText(
                context,
                "Permiso de cámara requerido para la linterna",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            transmisor.cerrar()
        }
    }

    BackHandler(onBack = onCerrar)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonApp(
                    texto = "Volver",
                    icono = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onCerrar
                )
                Text(
                    text = "MORSE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                palabras.forEachIndexed { index, palabra ->
                    if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                    val seleccionada = palabraSeleccionada.value == palabra
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (seleccionada) Primario else Superficie,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { palabraSeleccionada.value = palabra }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = palabra,
                            color = if (seleccionada) Color.White else TextoPrincipal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = palabraSeleccionada.value,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = textoAMorse(palabraSeleccionada.value),
                    fontSize = 22.sp,
                    letterSpacing = 6.sp,
                    color = Primario,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = if (transmitiendo.value) "TRANSMITIENDO…" else "Elegí modo y transmití",
                fontSize = 16.sp,
                fontWeight = if (transmitiendo.value) FontWeight.Bold else FontWeight.Normal,
                color = if (transmitiendo.value) Secundario else TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )

            if (!transmisor.flashDisponible) {
                Text(
                    text = "Linterna no disponible en este dispositivo",
                    color = Emergencia,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth()) {
                BotonApp(
                    texto = "Linterna",
                    icono = Icons.Outlined.FlashOn,
                    onClick = {
                        if (transmisor.flashDisponible) {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                transmisor.transmitirConLinterna(
                                    palabraSeleccionada.value, scope,
                                    onInicio = { transmitiendo.value = true },
                                    onFin = { transmitiendo.value = false }
                                )
                            } else {
                                permisoPendiente = palabraSeleccionada.value
                                launcher.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Linterna no disponible en este dispositivo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !transmitiendo.value,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BotonApp(
                    texto = "Sonido",
                    icono = Icons.Outlined.VolumeUp,
                    onClick = {
                        transmisor.transmitirConSonido(
                            palabraSeleccionada.value, scope,
                            onInicio = { transmitiendo.value = true },
                            onFin = { transmitiendo.value = false }
                        )
                    },
                    enabled = !transmitiendo.value,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}