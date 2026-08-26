package com.example.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthManager {
    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w("AuthManager", "Firebase Auth not available: ${e.message}")
            null
        }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Throwable) {
            null
        }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            Log.w("AuthManager", "Failed to sign out: ${e.message}")
        }
    }

    fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential, onComplete: (Boolean) -> Unit) {
        val authInstance = auth
        if (authInstance == null) {
            onComplete(false)
            return
        }
        try {
            authInstance.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
        } catch (e: Throwable) {
            onComplete(false)
        }
    }
}
