package me.joxquin.notivas.plugins.tecsup.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.joxquin.notivas.plugins.tecsup.api.TecsupEndpoints
import me.joxquin.notivas.plugins.tecsup.model.TecsupUser

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
        else TecsupSessionState.Connected(initialToken, null)
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
        _sessionState.value = TecsupSessionState.Connected(token, user)
        tokenStorage?.invoke(token)
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

    fun extractTokenFromRedirectUri(uriString: String): String? {
        if (!uriString.startsWith(TecsupEndpoints.REDIRECT_SCHEME)) return null

        // Format could be: com.tecsup.uexperience://login_auth?token=XYZ or #access_token=XYZ
        val queryOrFragment = uriString.substringAfter("?", "").ifEmpty {
            uriString.substringAfter("#", "")
        }
        if (queryOrFragment.isEmpty()) return null

        val params = queryOrFragment.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }

        return params["token"] ?: params["access_token"] ?: params["suite-token"]
    }
}
