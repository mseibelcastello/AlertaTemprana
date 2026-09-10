package com.example.alertatemprana.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.alertatemprana.ui.theme.AlertaTempranaTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.alertatemprana.R
import com.example.alertatemprana.ui.screens.CommunicationsScreen
import com.example.alertatemprana.ui.screens.HomeScreen
import com.example.alertatemprana.ui.screens.PersonalScreen
import kotlinx.coroutines.launch
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEachIndexed { index, destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
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
                1 -> HomeScreen()
                2 -> PersonalScreen()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    COMMUNICATIONS("Comunicaciones", R.drawable.ic_call),
    HOME("Home", R.drawable.ic_home),
    PERSONAL("Personal", R.drawable.ic_account_box),
}