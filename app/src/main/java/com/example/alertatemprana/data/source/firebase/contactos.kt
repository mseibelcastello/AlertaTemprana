package com.example.alertatemprana.data.source.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class ContactoEmergencia(
    val nombre: String,
    val telefono: String
)

class ContactosRepository {

    private val coleccion = FirebaseFirestore.getInstance()
        .collection("numeros_emergencia")

    fun escuchar(onContactos: (List<ContactoEmergencia>) -> Unit): ListenerRegistration {
        return coleccion.addSnapshotListener { snapshot, _ ->
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