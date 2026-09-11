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
