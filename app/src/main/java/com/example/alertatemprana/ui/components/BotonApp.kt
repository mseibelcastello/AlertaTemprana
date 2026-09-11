package com.example.alertatemprana.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alertatemprana.ui.theme.Advertencia
import com.example.alertatemprana.ui.theme.Emergencia
import com.example.alertatemprana.ui.theme.Primario
import com.example.alertatemprana.ui.theme.Secundario
import com.example.alertatemprana.ui.theme.Seguro

enum class VarianteBoton { PRIMARIO, SECUNDARIO, ADVERTENCIA, SEGURO, EMERGENCIA }

@Composable
fun BotonApp(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    enabled: Boolean = true,
    variante: VarianteBoton = VarianteBoton.PRIMARIO
) {
    val container = when (variante) {
        VarianteBoton.PRIMARIO -> Primario
        VarianteBoton.SECUNDARIO -> Secundario
        VarianteBoton.ADVERTENCIA -> Advertencia
        VarianteBoton.SEGURO -> Seguro
        VarianteBoton.EMERGENCIA -> Emergencia
    }
    val colors = ButtonDefaults.buttonColors(containerColor = container)

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(56.dp)
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.width(22.dp).height(22.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, fontSize = 16.sp)
    }
}