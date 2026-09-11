package com.example.alertatemprana.datos.firebase

import com.example.alertatemprana.modelos.RegistroUbicacion
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UbicacionesRepository {

    private val coleccion = FirebaseFirestore.getInstance()
        .collection("ubicaciones")

    fun guardar(
        latitud: Double,
        longitud: Double,
        direccion: String,
        onOk: () -> Unit,
        onError: (String) -> Unit
    ) {
        val datos = hashMapOf(
            "latitud" to latitud,
            "longitud" to longitud,
            "direccion" to direccion,
            "fecha" to FieldValue.serverTimestamp()
        )
        coleccion.add(datos)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al registrar")
            }
    }

    fun leerUltimas(
        limite: Int = 20,
        onListo: (List<RegistroUbicacion>) -> Unit,
        onError: (String) -> Unit
    ) {
        coleccion.orderBy("fecha", Query.Direction.DESCENDING)
            .limit(limite.toLong())
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.documents.mapNotNull { doc ->
                    val datos = doc.data ?: return@mapNotNull null
                    RegistroUbicacion(
                        direccion = datos["direccion"] as? String ?: "",
                        fecha = doc.getTimestamp("fecha")?.toDate()
                    )
                }
                onListo(lista)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al leer")
            }
    }
}