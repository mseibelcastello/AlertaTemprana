package com.example.alertatemprana.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.alertatemprana.ui.theme.AlertaTempranaTheme
import com.example.alertatemprana.ui.theme.Emergencia
import com.example.alertatemprana.ui.theme.Superficie
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.alertatemprana.data.source.device.AlertaDispositivo
import com.example.alertatemprana.data.source.firebase.AlertaCatastrofe
import com.example.alertatemprana.data.source.firebase.AlertasRepository
import com.example.alertatemprana.ui.screens.CommunicationsScreen
import com.example.alertatemprana.ui.screens.HomeScreen
import com.example.alertatemprana.ui.screens.PersonalScreen
import kotlinx.coroutines.launch
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.Toast
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

@Composable
fun PantallaAlerta(alerta: AlertaCatastrofe, onEnterado: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Emergencia)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "ALERTA ACTIVA",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = alerta.tipo,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = alerta.descripcion,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        androidx.compose.material3.Button(
            onClick = onEnterado,
            colors = ButtonDefaults.buttonColors(
                containerColor = Superficie,
                contentColor = Emergencia
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(top = 32.dp)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text(" Entendido", fontSize = 16.sp)
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