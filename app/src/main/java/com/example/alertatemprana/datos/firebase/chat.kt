package com.example.alertatemprana.datos.firebase

import com.example.alertatemprana.modelos.MensajeChat
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class ChatRepository {

    private val referencia = FirebaseDatabase.getInstance(
        "https://alertatemprana-47c0e-default-rtdb.firebaseio.com/"
    ).reference.child("mensajes")

    fun enviar(
        texto: String,
        emisor: String,
        onOk: () -> Unit,
        onError: (String) -> Unit
    ) {
        val datos = hashMapOf(
            "texto" to texto,
            "emisor" to emisor,
            "timestamp" to ServerValue.TIMESTAMP
        )
        referencia.push()
            .setValue(datos)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al enviar")
            }
    }

    fun escuchar(onMensaje: (MensajeChat) -> Unit): ChildEventListener {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val datos = snapshot.value as? Map<*, *> ?: return
                onMensaje(
                    MensajeChat(
                        id = snapshot.key ?: "",
                        texto = datos["texto"] as? String ?: "",
                        emisor = datos["emisor"] as? String ?: "",
                        timestamp = (datos["timestamp"] as? Long) ?: 0L
                    )
                )
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        }
        referencia.limitToLast(100).addChildEventListener(listener)
        return listener
    }

    fun detener(listener: ChildEventListener) {
        referencia.removeEventListener(listener)
    }
}