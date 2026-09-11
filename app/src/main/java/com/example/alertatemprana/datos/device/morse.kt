package com.example.alertatemprana.datos.device

const val PUNTO_MS = 200L
const val RAYA_MS = 600L
const val PAUSA_SIMBOLO_MS = 200L
const val PAUSA_LETRA_MS = 600L
const val PAUSA_PALABRA_MS = 1400L

val MORSE = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..",
    'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
    'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
    'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
    '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----."
)

fun textoAMorse(texto: String): String = texto
    .uppercase()
    .map { MORSE[it] ?: "" }
    .filter { it.isNotBlank() }
    .joinToString(" ")

fun generarSecuencia(morse: String): List<Pair<Long, Boolean>> {
    val secuencia = mutableListOf<Pair<Long, Boolean>>()
    var esPrimerElemento = true
    for (simbolo in morse) {
        when (simbolo) {
            '.' -> {
                if (!esPrimerElemento) secuencia.add(PAUSA_SIMBOLO_MS to false)
                secuencia.add(PUNTO_MS to true)
                esPrimerElemento = false
            }
            '-' -> {
                if (!esPrimerElemento) secuencia.add(PAUSA_SIMBOLO_MS to false)
                secuencia.add(RAYA_MS to true)
                esPrimerElemento = false
            }
            ' ' -> {
                secuencia.add(PAUSA_LETRA_MS to false)
                esPrimerElemento = true
            }
        }
    }
    return secuencia
}