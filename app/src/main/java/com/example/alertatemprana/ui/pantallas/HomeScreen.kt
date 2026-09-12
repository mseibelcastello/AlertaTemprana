package com.example.alertatemprana.ui.pantallas

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
import com.example.alertatemprana.datos.device.AsistenteVoz
import com.example.alertatemprana.datos.device.Bateria
import com.example.alertatemprana.datos.device.GrabadorAudio
import com.example.alertatemprana.datos.device.GrabadorVideo
import com.example.alertatemprana.datos.device.Linterna
import com.example.alertatemprana.datos.device.TransmisorMorse
import com.example.alertatemprana.datos.device.Ubicacion
import com.example.alertatemprana.datos.device.interpretarComando
import com.example.alertatemprana.datos.device.textoAMorse
import com.example.alertatemprana.datos.firebase.ChatRepository
import com.example.alertatemprana.datos.firebase.ContactosRepository
import com.example.alertatemprana.datos.firebase.UbicacionesRepository
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
import com.example.alertatemprana.modelos.AlertaCatastrofe
import com.example.alertatemprana.modelos.ClimaActual
import com.example.alertatemprana.modelos.ComandoVoz
import com.example.alertatemprana.modelos.ContactoEmergencia
import com.example.alertatemprana.modelos.MensajeChat
import com.example.alertatemprana.modelos.RegistroUbicacion
import com.example.alertatemprana.modelos.guias
import com.example.alertatemprana.datos.buscarDireccion
import com.example.alertatemprana.datos.obtenerDireccionExacta
import com.example.alertatemprana.datos.obtenerNombreLugar
import com.example.alertatemprana.datos.parsearClimaActual
import com.example.alertatemprana.datos.pedirHttp

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

                    TileApp(
                        etiqueta = "Consejos",
                        dato = "Guía",
                        detalle = "Cómo actuar ante cada catástrofe",
                        colorFondo = Superficie,
                        colorTexto = TextoPrincipal,
                        detalleColor = TextoSecundario,
                        onClick = { mostrarGuia.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 120.dp)
                    )

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
