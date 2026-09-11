package com.example.alertatemprana.ui

import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.alertatemprana.datos.device.AlertaDispositivo
import com.example.alertatemprana.datos.firebase.AlertasRepository
import com.example.alertatemprana.modelos.AlertaCatastrofe
import com.example.alertatemprana.ui.pantallas.CommunicationsScreen
import com.example.alertatemprana.ui.pantallas.HomeScreen
import com.example.alertatemprana.ui.pantallas.PantallaAlerta
import com.example.alertatemprana.ui.pantallas.PersonalScreen
import com.example.alertatemprana.ui.theme.AlertaTempranaTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPrefs)
        Configuration.getInstance().userAgentValue =
            "AlertaTemprana/1.0 (mseibelcastello@gmail.com)"
        enableEdgeToEdge()
        setContent {
            AlertaTempranaTheme {
                AlertaTempranaApp()
            }
        }
    }

    //apaga la linterna al cerrar la app
    override fun onStop() {
        super.onStop()
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as
                    CameraManager
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }?.let { cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) { }
    }
}

@PreviewScreenSizes
@Composable
fun AlertaTempranaApp() {
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()
    val contexto = LocalContext.current
    val alertasRepository = remember { AlertasRepository() }
    val alertaDispositivo = remember { AlertaDispositivo(contexto) }
    val alertaActiva = remember { mutableStateOf<AlertaCatastrofe?>(null) }
    val alertaReconocida = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var avisoMostrado = false
        val registro = alertasRepository.escucharActiva(
            onAlerta = { alerta ->
                val anterior = alertaActiva.value
                alertaActiva.value = alerta
                if (alerta == null) {
                    alertaReconocida.value = false
                } else if (!alertaReconocida.value &&
                    (anterior == null || anterior.id != alerta.id)) {
                    alertaDispositivo.activar()
                }
            },
            onError = { error ->
                if (!avisoMostrado) {
                    avisoMostrado = true
                    Toast.makeText(
                        contexto,
                        "Error de alertas (¿reglas?): ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
        onDispose {
            registro.remove()
            alertaDispositivo.desactivar()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    item(
                        icon = {
                            Icon(
                                destination.icono,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) { page ->
                when (page) {
                    0 -> CommunicationsScreen()
                    1 -> HomeScreen(
                        alerta = alertaActiva.value,
                        onIrAComunicaciones = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onIrAPersonal = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                    2 -> PersonalScreen()
                }
            }
        }

        if (alertaActiva.value != null && !alertaReconocida.value) {
            val alerta = alertaActiva.value
            if (alerta != null) {
                PantallaAlerta(
                    alerta = alerta,
                    onEnterado = {
                        alertaDispositivo.desactivar()
                        alertaReconocida.value = true
                    }
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icono: ImageVector,
) {
    COMMUNICATIONS("Comunicaciones", Icons.Outlined.Call),
    HOME("Home", Icons.Outlined.Home),
    PERSONAL("Personal", Icons.Outlined.Person),
}