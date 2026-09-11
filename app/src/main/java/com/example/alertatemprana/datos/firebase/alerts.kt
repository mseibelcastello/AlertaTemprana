package com.example.alertatemprana.datos.firebase

import android.util.Log
import com.example.alertatemprana.modelos.AlertaCatastrofe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AlertasRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("alertas")

    fun escucharActiva(
        onAlerta: (AlertaCatastrofe?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return coleccion.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AlertaTemprana", "Error escuchando alerts", error)
                onError(error)
                return@addSnapshotListener
            }
            val alerta = snapshot?.documents
                ?.map { doc ->
                    val d = doc.data ?: return@map null
                    val estado = d["estado"]
                    val activa = when (estado) {
                        is Boolean -> estado
                        is String -> estado.equals("true", ignoreCase = true)
                        else -> false
                    }
                    AlertaCatastrofe(
                        id = doc.id,
                        tipo = (d["tipo"] as? String) ?: "",
                        descripcion = (d["descripcion"] as? String) ?: "",
                        activa = activa
                    )
                }
                ?.firstOrNull { it?.activa == true }
            onAlerta(alerta)
        }
    }
}