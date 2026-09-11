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
