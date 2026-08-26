package com.example.model

import com.google.firebase.firestore.DocumentId

data class SavedMac(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val macAddress: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
