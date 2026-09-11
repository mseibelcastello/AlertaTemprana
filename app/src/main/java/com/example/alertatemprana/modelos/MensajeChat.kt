package com.example.alertatemprana.modelos

data class MensajeChat(
    val id: String,
    val texto: String,
    val emisor: String,
    val timestamp: Long
)