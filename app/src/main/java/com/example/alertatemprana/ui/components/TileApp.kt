package com.example.alertatemprana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TileApp(
    dato: String,
    etiqueta: String? = null,
    detalle: String? = null,
    colorFondo: Color,
    colorTexto: Color = Color.White,
    detalleColor: Color = colorTexto.copy(alpha = 0.84f),
    datoTamanio: TextUnit = 26.sp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val contenido: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (etiqueta != null) {
                Text(
                    text = etiqueta.uppercase(),
                    color = colorTexto.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = dato,
                color = colorTexto,
                fontSize = datoTamanio,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    color = detalleColor,
                    fontSize = 13.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    val base = modifier
        .background(colorFondo)
        .heightIn(min = 96.dp)

    if (onClick != null) {
        Box(modifier = base.clickable(onClick = onClick)) { contenido() }
    } else {
        Box(modifier = base) { contenido() }
    }
}