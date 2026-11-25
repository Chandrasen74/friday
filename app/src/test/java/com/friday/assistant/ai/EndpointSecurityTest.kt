package com.friday.assistant.ai

import com.friday.assistant.data.ApiEndpointConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointSecurityTest {
    private val dummyKey = "sk-HARNESS-DUMMY-NOT-A-REAL-KEY-0001"

    @Test
    fun normalizeOrigin_httpsUsesDefaultPortAndCanonicalizesCase() {
        assertEquals(
            EndpointSecurity.Origin("https", "api.example.com", 443),
            EndpointSecurity.normalizeOrigin("https://api.example.com/v1")
        )
        assertEquals(
            EndpointSecurity.Origin("https", "api.example.com", 8443),
            EndpointSecurity.normalizeOrigin("https://api.example.com:8443/v1")
        )
        assertEquals(
            EndpointSecurity.Origin("https", "api.example.com", 443),
            EndpointSecurity.normalizeOrigin("HTTPS://API.EXAMPLE.COM/v1")
        )
        assertEquals(
            EndpointSecurity.Origin("http", "api.example.com", 80),
            EndpointSecurity.normalizeOrigin("http://api.example.com/v1")
        )
    }

    @Test
    fun normalizeOrigin_rejectsUnsupportedAndMalformedUrls() {
        assertNull(EndpointSecurity.normalizeOrigin("   "))
        assertNull(EndpointSecurity.normalizeOrigin("/v1/chat"))
        assertNull(EndpointSecurity.normalizeOrigin("ftp://api.example.com/v1"))
        assertNull(EndpointSecurity.normalizeOrigin("https://user:password@api.example.com/v1"))
        assertNull(EndpointSecurity.normalizeOrigin("https:///v1/chat"))
    }

    @Test
    fun isSecureEndpoint_onlyAcceptsAbsoluteHttps() {
        assertTrue(EndpointSecurity.isSecureEndpoint("https://api.example.com/v1"))
        assertFalse(EndpointSecurity.isSecureEndpoint("http://api.example.com/v1"))
        assertFalse(EndpointSecurity.isSecureEndpoint("not a URL"))
    }

    @Test
    fun requireSecureEndpoint_rejectsCleartextAndMalformedUrls() {
        val cleartext = assertThrows(IllegalArgumentException::class.java) {
            EndpointSecurity.requireSecureEndpoint("http://api.example.com/v1")
        }
        assertTrue(cleartext.message.orEmpty().contains("Cleartext"))
        assertTrue(cleartext.message.orEmpty().contains("API key"))

        val malformed = assertThrows(IllegalArgumentException::class.java) {
            EndpointSecurity.requireSecureEndpoint("not a URL")
        }
        assertTrue(malformed.message.orEmpty().contains("https"))

        EndpointSecurity.requireSecureEndpoint("https://api.example.com/v1")
    }

    @Test
    fun remoteRequestPolicy_rejectsHttpBeforeCheckingCredentialBinding() {
        val config = ApiEndpointConfig(
            baseUrl = "http://api.example.com/v1",
            apiKey = dummyKey,
            apiKeyOrigin = "https://api.example.com:443"
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            RemoteRequestPolicy.validate(config, "http://api.example.com/v1/chat/completions")
        }
        assertTrue(error.message.orEmpty().contains("Cleartext"))
    }

    @Test
    fun remoteRequestPolicy_requiresMatchingOriginWhenKeyIsPresent() {
        val endpoint = "https://api.example.com/v1/chat/completions"

        val blankOrigin = assertThrows(IllegalArgumentException::class.java) {
            RemoteRequestPolicy.validate(
                ApiEndpointConfig(baseUrl = "https://api.example.com/v1", apiKey = dummyKey),
                endpoint
            )
        }
        assertTrue(blankOrigin.message.orEmpty().contains("Re-enter"))

        val wrongOrigin = assertThrows(IllegalArgumentException::class.java) {
            RemoteRequestPolicy.validate(
                ApiEndpointConfig(
                    baseUrl = "https://api.example.com/v1",
                    apiKey = dummyKey,
                    apiKeyOrigin = "https://other.example.com:443"
                ),
                endpoint
            )
        }
        assertTrue(wrongOrigin.message.orEmpty().contains("https://other.example.com:443"))
        assertTrue(wrongOrigin.message.orEmpty().contains("https://api.example.com:443"))
        assertTrue(wrongOrigin.message.orEmpty().contains("Re-enter"))

        RemoteRequestPolicy.validate(
            ApiEndpointConfig(
                baseUrl = "https://api.example.com/v1",
                apiKey = dummyKey,
                apiKeyOrigin = "https://api.example.com:443"
            ),
            endpoint
        )
    }

    @Test
    fun remoteRequestPolicy_blankKeyOnlyRequiresHttps() {
        RemoteRequestPolicy.validate(
            ApiEndpointConfig(baseUrl = "https://api.example.com/v1", apiKey = ""),
            "https://api.example.com/v1/chat/completions"
        )
    }

    @Test
    fun credentialBinding_neverRebindsAStoredOrigin() {
        assertEquals("", CredentialBinding.bindOrigin("https://api.example.com/v1", "", ""))
        assertEquals(
            "https://api.example.com:443",
            CredentialBinding.bindOrigin("https://api.example.com/v1", dummyKey, "")
        )
        assertEquals(
            "https://original.example.com:443",
            CredentialBinding.bindOrigin("https://new.example.com/v1", dummyKey, "https://original.example.com:443")
        )
        assertEquals("", CredentialBinding.bindOrigin("", dummyKey, ""))
        assertEquals("", CredentialBinding.bindOrigin("not a URL", dummyKey, ""))
    }

    @Test
    fun remoteRequestPolicy_rejectRedirectsRejects3xxAndBoundsLocation() {
        val location = "https://redirect.example/" + "L".repeat(250)
        val error = assertThrows(IllegalStateException::class.java) {
            RemoteRequestPolicy.rejectRedirects(307, location)
        }
        val message = error.message.orEmpty()

        assertTrue(message.contains("API 307"))
        assertTrue(message.contains("Automatic redirects are disabled"))
        assertTrue(message.contains("configure the final endpoint URL directly"))
        assertTrue(message.contains(location.take(200)))
        assertFalse(message.contains(location.take(201)))

        RemoteRequestPolicy.rejectRedirects(200, null)
    }

    @Test
    fun credentialBinding_onEndpointChangedHandlesEndpointChanges() {
        val boundOrigin = "https://api.example.com:443"

        val blankKey = CredentialBinding.onEndpointChanged(
            currentKey = "   ",
            currentBoundOrigin = boundOrigin,
            newUrl = "http://api.example.com/v1"
        )
        assertEquals("   ", blankKey.apiKey)
        assertEquals(boundOrigin, blankKey.apiKeyOrigin)
        assertNull(blankKey.message)

        val cleartext = CredentialBinding.onEndpointChanged(
            currentKey = dummyKey,
            currentBoundOrigin = boundOrigin,
            newUrl = "http://api.example.com/v1"
        )
        assertEquals("", cleartext.apiKey)
        assertEquals("", cleartext.apiKeyOrigin)
        assertEquals(
            "Endpoint URL must use https:// (for example https://api.example.com/v1). Cleartext http:// is not allowed.",
            cleartext.message
        )

        val changedOrigin = CredentialBinding.onEndpointChanged(
            currentKey = dummyKey,
            currentBoundOrigin = boundOrigin,
            newUrl = "https://other.example.com/v1"
        )
        assertEquals("", changedOrigin.apiKey)
        assertEquals("", changedOrigin.apiKeyOrigin)
        assertEquals(
            "Endpoint origin changed. The saved API key was cleared. Re-enter the API key for the new host.",
            changedOrigin.message
        )

        val sameOrigin = CredentialBinding.onEndpointChanged(
            currentKey = dummyKey,
            currentBoundOrigin = boundOrigin,
            newUrl = "https://api.example.com/v2"
        )
        assertEquals(dummyKey, sameOrigin.apiKey)
        assertEquals(boundOrigin, sameOrigin.apiKeyOrigin)
        assertNull(sameOrigin.message)

        val unparseable = CredentialBinding.onEndpointChanged(
            currentKey = dummyKey,
            currentBoundOrigin = boundOrigin,
            newUrl = "not a URL"
        )
        assertEquals(dummyKey, unparseable.apiKey)
        assertEquals(boundOrigin, unparseable.apiKeyOrigin)
        assertNull(unparseable.message)
    }
}
