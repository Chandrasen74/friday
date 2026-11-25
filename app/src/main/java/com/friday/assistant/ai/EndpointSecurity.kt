package com.friday.assistant.ai

import com.friday.assistant.data.ApiEndpointConfig
import java.net.URI

object EndpointSecurity {
    const val SCHEME_HTTPS = "https"
    const val SCHEME_HTTP = "http"
    const val DEFAULT_HTTPS_PORT = 443
    const val DEFAULT_HTTP_PORT = 80

    data class Origin(val scheme: String, val host: String, val port: Int) {
        val canonical: String get() = "$scheme://$host:$port"
    }

    fun normalizeOrigin(url: String): Origin? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != SCHEME_HTTPS && scheme != SCHEME_HTTP) return null
        if (!uri.userInfo.isNullOrEmpty()) return null

        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        val port = when {
            uri.port > 0 -> uri.port
            scheme == SCHEME_HTTPS -> DEFAULT_HTTPS_PORT
            else -> DEFAULT_HTTP_PORT
        }
        return Origin(scheme = scheme, host = host, port = port)
    }

    fun isSecureEndpoint(url: String): Boolean =
        normalizeOrigin(url)?.scheme == SCHEME_HTTPS

    fun requireSecureEndpoint(url: String) {
        val origin = normalizeOrigin(url)
            ?: throw IllegalArgumentException("Endpoint URL must be an absolute https:// URL with a valid host.")
        if (origin.scheme == SCHEME_HTTP) {
            throw IllegalArgumentException(
                "Cleartext http:// endpoints are not allowed because they would expose the API key and chat context. Use https:// instead."
            )
        }
        require(origin.scheme == SCHEME_HTTPS) { "Endpoint URL must use https://." }
    }
}

object RemoteRequestPolicy {
    fun validate(config: ApiEndpointConfig, endpoint: String) {
        EndpointSecurity.requireSecureEndpoint(endpoint)

        if (config.apiKey.trim().isBlank()) return

        val boundOrigin = config.apiKeyOrigin.trim()
        if (boundOrigin.isBlank()) {
            throw IllegalArgumentException(
                "The API key is not bound to an endpoint origin. Re-enter the API key in Settings."
            )
        }

        val endpointOrigin = EndpointSecurity.normalizeOrigin(endpoint)?.canonical
            ?: throw IllegalArgumentException("Endpoint URL must be an absolute https:// URL with a valid host.")
        if (boundOrigin != endpointOrigin) {
            throw IllegalArgumentException(
                "The API key is bound to '$boundOrigin', but the endpoint origin is '$endpointOrigin'. Re-enter the API key in Settings."
            )
        }
    }

    fun rejectRedirects(code: Int, location: String?) {
        if (code !in 300..399) return
        val target = location?.take(200).orEmpty()
        throw IllegalStateException(
            "API $code: the endpoint redirected to '$target'. Automatic redirects are disabled; configure the final endpoint URL directly."
        )
    }
}

object CredentialBinding {
    data class CredentialChange(
        val apiKey: String,
        val apiKeyOrigin: String,
        val message: String?
    )

    fun bindOrigin(baseUrl: String, apiKey: String, storedOrigin: String): String {
        if (apiKey.trim().isBlank()) return ""
        if (storedOrigin.trim().isNotBlank()) return storedOrigin.trim()
        return EndpointSecurity.normalizeOrigin(baseUrl)?.canonical ?: ""
    }

    fun onEndpointChanged(currentKey: String, currentBoundOrigin: String, newUrl: String): CredentialChange {
        if (currentKey.isBlank()) {
            return CredentialChange(currentKey, currentBoundOrigin, null)
        }

        val newOrigin = EndpointSecurity.normalizeOrigin(newUrl)
        if (newOrigin?.scheme == EndpointSecurity.SCHEME_HTTP) {
            return CredentialChange(
                apiKey = "",
                apiKeyOrigin = "",
                message = "Endpoint URL must use https:// (for example https://api.example.com/v1). Cleartext http:// is not allowed."
            )
        }

        if (currentBoundOrigin.isNotBlank() && newOrigin != null && newOrigin.canonical != currentBoundOrigin) {
            return CredentialChange(
                apiKey = "",
                apiKeyOrigin = "",
                message = "Endpoint origin changed. The saved API key was cleared. Re-enter the API key for the new host."
            )
        }

        return CredentialChange(currentKey, currentBoundOrigin, null)
    }
}
