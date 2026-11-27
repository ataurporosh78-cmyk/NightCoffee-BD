package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Context.localAuthDataStore by preferencesDataStore(name = "nightcoffee_local_auth")

/** Identity stored on this device. Local profiles and guest sessions are never verified Google accounts. */
data class LocalUser(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val kind: Kind = Kind.EMAIL
) {
    enum class Kind { EMAIL, LOCAL_PROFILE, GUEST }

    val statusLabel: String get() = when (kind) {
        Kind.EMAIL -> "Local account · saved on this device"
        Kind.LOCAL_PROFILE -> "Local profile · not a verified Google sign-in"
        Kind.GUEST -> "Guest · saved on this device"
    }
}

class LocalAuthRepository(private val context: Context) {
    private val accountsKey = stringPreferencesKey("accounts_v1")
    private val sessionKey = stringPreferencesKey("active_session_v1")

    val session: Flow<LocalUser?> = context.localAuthDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[sessionKey]?.let(::decodeUser) }

    suspend fun createAccount(name: String, email: String, password: CharArray): LocalUser {
        val cleanEmail = email.trim().lowercase()
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        try {
            val passwordHash = withContext(Dispatchers.Default) { hashPassword(password, salt) }
            val user = LocalUser("local-${UUID.randomUUID()}", name.trim(), cleanEmail)
            context.localAuthDataStore.edit { prefs ->
                val accounts = runCatching { JSONObject(prefs[accountsKey].orEmpty()) }.getOrDefault(JSONObject())
                if (accounts.has(cleanEmail)) throw IllegalStateException("An account already exists with this email. Sign in instead.")
                accounts.put(cleanEmail, JSONObject().apply {
                    put("id", user.id)
                    put("name", user.displayName)
                    put("salt", salt.toHex())
                    put("hash", passwordHash)
                })
                prefs[accountsKey] = accounts.toString()
                prefs[sessionKey] = encodeUser(user)
            }
            return user
        } finally {
            password.fill('\u0000')
        }
    }

    suspend fun signIn(email: String, password: CharArray): LocalUser {
        try {
            val cleanEmail = email.trim().lowercase()
            val prefs = context.localAuthDataStore.data.catch { emit(emptyPreferences()) }.first()
            val account = runCatching { JSONObject(prefs[accountsKey].orEmpty()).optJSONObject(cleanEmail) }.getOrNull()
                ?: throw IllegalArgumentException("No local account found with this email. Switch to Create Account to sign up.")
            val salt = runCatching { account.getString("salt").hexToBytes() }.getOrElse {
                throw IllegalStateException("This saved account cannot be read. Create a new local account.")
            }
            val expected = account.optString("hash")
            val actual = withContext(Dispatchers.Default) { hashPassword(password, salt) }
            if (!constantTimeEquals(expected, actual)) {
                throw IllegalArgumentException("Incorrect email or password. Please try again.")
            }
            val user = LocalUser(account.optString("id"), account.optString("name", cleanEmail.substringBefore('@')), cleanEmail)
            context.localAuthDataStore.edit { it[sessionKey] = encodeUser(user) }
            return user
        } finally {
            password.fill('\u0000')
        }
    }

    suspend fun continueWithLocalProfile(): LocalUser {
        val user = LocalUser("local-profile", "Night Coffee Local Profile", kind = LocalUser.Kind.LOCAL_PROFILE)
        context.localAuthDataStore.edit { it[sessionKey] = encodeUser(user) }
        return user
    }

    suspend fun continueAsGuest(): LocalUser {
        val user = LocalUser("guest-${System.currentTimeMillis()}", "Guest Coffee Lover", kind = LocalUser.Kind.GUEST)
        context.localAuthDataStore.edit { it[sessionKey] = encodeUser(user) }
        return user
    }

    suspend fun signOut() {
        context.localAuthDataStore.edit { it.remove(sessionKey) }
    }

    private fun encodeUser(user: LocalUser): String = JSONObject().apply {
        put("id", user.id); put("name", user.displayName); put("email", user.email)
        put("kind", user.kind.name)
    }.toString()

    private fun decodeUser(encoded: String): LocalUser? = runCatching {
        val json = JSONObject(encoded)
        LocalUser(
            id = json.getString("id"),
            displayName = json.optString("name", "Coffee Lover"),
            email = json.optString("email").takeIf { it.isNotBlank() && it != "null" },
            kind = runCatching { LocalUser.Kind.valueOf(json.optString("kind", "EMAIL")) }.getOrDefault(LocalUser.Kind.EMAIL)
        )
    }.getOrNull()

    private fun hashPassword(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.toHex()
        } finally { spec.clearPassword() }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

}
