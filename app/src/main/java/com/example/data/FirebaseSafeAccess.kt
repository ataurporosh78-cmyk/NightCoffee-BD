package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSafeAccess {
    private const val TAG = "FirebaseSafeAccess"

    fun ensureInitialized(context: Context): Boolean {
        val appContext = context.applicationContext
        return try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                true
            } else {
                FirebaseApp.initializeApp(appContext) != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization unavailable: ${e.message}")
            false
        }
    }

    fun safeAuth(context: Context): FirebaseAuth? {
        if (!ensureInitialized(context)) return null
        return try {
            Firebase.auth
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    fun safeFirestore(context: Context): FirebaseFirestore? {
        if (!ensureInitialized(context)) return null
        val appContext = context.applicationContext
        return try {
            val databaseId = runCatching {
                appContext.getString(R.string.firestore_database_id).trim()
            }.getOrDefault("")
            if (databaseId.isEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance(databaseId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }
    }

    fun hasValidGoogleWebClientId(context: Context): Boolean {
        val clientId = runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrDefault("")
        return clientId.isNotEmpty() && clientId != "placeholder_web_client_id"
    }
}
