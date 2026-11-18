package me.joxquin.notivas.plugins.tecsup.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import me.joxquin.notivas.plugins.tecsup.api.TecsupEndpoints
import me.joxquin.notivas.plugins.tecsup.model.TecsupUser
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed interface TecsupSessionState {
    data object Disconnected : TecsupSessionState
    data object Loading : TecsupSessionState
    data class Connected(val token: String, val user: TecsupUser?) : TecsupSessionState
    data class Expired(val lastUser: TecsupUser?) : TecsupSessionState
}

class TecsupSessionManager(
    private val tokenStorage: (suspend (String?) -> Unit)? = null,
    initialToken: String? = null
) {
    private val _sessionState = MutableStateFlow<TecsupSessionState>(
        if (initialToken.isNullOrBlank()) TecsupSessionState.Disconnected
        else TecsupSessionState.Connected(initialToken, parseUserFromToken(initialToken))
    )
    val sessionState: StateFlow<TecsupSessionState> = _sessionState.asStateFlow()

    val currentToken: String?
        get() = when (val state = _sessionState.value) {
            is TecsupSessionState.Connected -> state.token
            else -> null
        }

    val isConnected: Boolean
        get() = _sessionState.value is TecsupSessionState.Connected

    suspend fun saveToken(token: String, user: TecsupUser? = null) {
        val cleaned = cleanToken(token)
        val resolvedUser = user ?: parseUserFromToken(cleaned)
        _sessionState.value = TecsupSessionState.Connected(cleaned, resolvedUser)
        tokenStorage?.invoke(cleaned)
    }

    suspend fun markExpired() {
        val lastUser = when (val state = _sessionState.value) {
            is TecsupSessionState.Connected -> state.user
            is TecsupSessionState.Expired -> state.lastUser
            else -> null
        }
        _sessionState.value = TecsupSessionState.Expired(lastUser)
    }

    suspend fun logout() {
        _sessionState.value = TecsupSessionState.Disconnected
        tokenStorage?.invoke(null)
    }

    fun cleanToken(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith(TecsupEndpoints.REDIRECT_SCHEME) || trimmed.contains("idUserToken=")) {
            extractTokenFromRedirectUri(trimmed)?.let { return it }
        }
        return extractTokenFromJsonOrBase64(trimmed) ?: trimmed
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun extractTokenFromJsonOrBase64(raw: String): String? {
        val trimmed = raw.trim()
        val jsonPattern = """\"token\"\s*:\s*\"([^\"]+)\"""".toRegex()

        // 1. Direct JSON regex
        jsonPattern.find(trimmed)?.groupValues?.get(1)?.let { return it }

        // 2. Base64 decoded check (Default and UrlSafe)
        for (base64Format in listOf(Base64.Default, Base64.UrlSafe)) {
            try {
                val decoded = base64Format.decode(trimmed).decodeToString()
                jsonPattern.find(decoded)?.groupValues?.get(1)?.let { return it }
                if (decoded.startsWith("eyJ") && decoded.count { it == '.' } == 2) {
                    return decoded
                }
            } catch (_: Exception) {
            }
        }

        // 3. Direct JWT check
        if (trimmed.startsWith("eyJ") && trimmed.count { it == '.' } == 2) {
            return trimmed
        }

        return null
    }

    fun extractTokenFromRedirectUri(uriString: String): String? {
        if (!uriString.startsWith(TecsupEndpoints.REDIRECT_SCHEME) && !uriString.contains("idUserToken=")) return null

        val queryOrFragment = uriString.substringAfter("?", "").ifEmpty {
            uriString.substringAfter("#", "")
        }
        if (queryOrFragment.isEmpty()) return null

        val params = queryOrFragment.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }

        val rawToken = params["idUserToken"]
            ?: params["token"]
            ?: params["access_token"]
            ?: params["suite-token"]
            ?: params["id_token"]
            ?: return null

        return extractTokenFromJsonOrBase64(rawToken) ?: rawToken
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun parseUserFromToken(token: String): TecsupUser? {
        val cleaned = cleanToken(token)
        val parts = cleaned.split(".")
        if (parts.size < 2) return null
        val payload = parts[1]
        val padded = when (payload.length % 4) {
            2 -> "$payload=="
            3 -> "$payload="
            else -> payload
        }

        val decodedString = try {
            Base64.UrlSafe.decode(padded).decodeToString()
        } catch (_: Exception) {
            try {
                Base64.Default.decode(padded).decodeToString()
            } catch (_: Exception) {
                return null
            }
        }

        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val obj = json.decodeFromString(JsonObject.serializer(), decodedString)
            val name = obj["name"]?.jsonPrimitive?.content
                ?: run {
                    val given = obj["given_name"]?.jsonPrimitive?.content.orEmpty()
                    val family = obj["family_name"]?.jsonPrimitive?.content.orEmpty()
                    "$given $family".trim()
                }.ifEmpty { "Estudiante Tecsup" }

            val email = obj["email"]?.jsonPrimitive?.content
                ?: obj["emails"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                ?: ""

            val sub = obj["sub"]?.jsonPrimitive?.content
                ?: obj["oid"]?.jsonPrimitive?.content
                ?: ""

            val studentCode = if (email.contains("@")) {
                email.substringBefore("@")
            } else {
                sub.take(8)
            }

            TecsupUser(
                id = sub,
                name = name,
                email = email,
                studentCode = studentCode,
                campus = "Tecsup",
                career = "Estudiante"
            )
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getTokenExpirationTimestamp(token: String): Long? {
        val cleaned = cleanToken(token)
        val parts = cleaned.split(".")
        if (parts.size < 2) return null
        val payload = parts[1]
        val padded = when (payload.length % 4) {
            2 -> "$payload=="
            3 -> "$payload="
            else -> payload
        }
        val decodedString = try {
            Base64.UrlSafe.decode(padded).decodeToString()
        } catch (_: Exception) {
            try {
                Base64.Default.decode(padded).decodeToString()
            } catch (_: Exception) {
                return null
            }
        }
        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val obj = json.decodeFromString(JsonObject.serializer(), decodedString)
            obj["exp"]?.jsonPrimitive?.longOrNull
        } catch (_: Exception) {
            null
        }
    }
}
