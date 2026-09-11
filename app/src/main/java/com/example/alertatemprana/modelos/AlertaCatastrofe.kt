package com.example.alertatemprana.modelos

data class AlertaCatastrofe(
    val id: String,
    val tipo: String,
    val descripcion: String,
    val activa: Boolean
)