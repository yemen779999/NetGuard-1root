package com.example.network

import android.util.Log
import com.example.model.SavedMac
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreManager {
    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w("FirestoreManager", "Firebase Firestore not available: ${e.message}")
            null
        }

    private const val COLLECTION_MAC = "saved_macs"

    suspend fun saveMac(savedMac: SavedMac) {
        val firestore = db ?: return
        try {
            firestore.collection(COLLECTION_MAC).add(savedMac).await()
        } catch (e: Throwable) {
            Log.w("FirestoreManager", "Firestore saveMac error: ${e.message}")
        }
    }

    suspend fun getSavedMacs(userId: String): List<SavedMac> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection(COLLECTION_MAC)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(SavedMac::class.java)
        } catch (e: Throwable) {
            Log.w("FirestoreManager", "Firestore getSavedMacs error: ${e.message}")
            emptyList()
        }
    }
}
