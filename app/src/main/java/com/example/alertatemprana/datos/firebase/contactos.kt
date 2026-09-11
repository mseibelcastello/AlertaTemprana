package com.example.alertatemprana.datos.firebase

import com.example.alertatemprana.modelos.ContactoEmergencia
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ContactosRepository {

    private val coleccion = FirebaseFirestore.getInstance()
        .collection("numeros_emergencia")

    fun escuchar(
        onContactos: (List<ContactoEmergencia>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return coleccion.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents?.mapNotNull { doc ->
                val datos = doc.data ?: return@mapNotNull null
                ContactoEmergencia(
                    nombre = datos["nombre"] as? String ?: "",
                    telefono = datos["numero"] as? String ?: ""
                )
            } ?: emptyList()
            onContactos(lista)
        }
    }
}