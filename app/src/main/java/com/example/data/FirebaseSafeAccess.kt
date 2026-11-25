package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

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

    fun safeStorage(context: Context): FirebaseStorage? {
        if (!ensureInitialized(context)) return null
        return try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage unavailable: ${e.message}")
            null
        }
    }

    fun safeFunctions(context: Context): FirebaseFunctions? {
        if (!ensureInitialized(context)) return null
        return try {
            FirebaseFunctions.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFunctions unavailable: ${e.message}")
            null
        }
    }

    fun getGoogleWebClientId(context: Context): String? {
        return runCatching {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resId == 0) return@runCatching null
            context.getString(resId).trim().takeIf {
                it.isNotEmpty() && it != "placeholder_web_client_id"
            }
        }.getOrNull()
    }

    fun hasValidGoogleWebClientId(context: Context): Boolean {
        return getGoogleWebClientId(context) != null
    }
}
