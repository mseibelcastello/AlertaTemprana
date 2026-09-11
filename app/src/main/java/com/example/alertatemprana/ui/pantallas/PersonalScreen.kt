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
