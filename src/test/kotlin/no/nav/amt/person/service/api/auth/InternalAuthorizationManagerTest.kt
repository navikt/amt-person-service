package no.nav.amt.person.service.api.auth

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.access.intercept.RequestAuthorizationContext

class InternalAuthorizationManagerTest {
    private val authorizationManager = InternalAuthorizationManager()

    @Test
    fun `authorize - loopback adresse - returnerer true`() {
        val request = MockHttpServletRequest().apply {
            remoteAddr = "127.0.0.1"
        }
        val context = RequestAuthorizationContext(request)

        authorizationManager.authorize({ TestingAuthenticationToken("any", null) }, context).isGranted shouldBe true
    }

    @Test
    fun `authorize - ekstern adresse - returnerer false`() {
        val request = MockHttpServletRequest().apply {
            remoteAddr = "10.0.0.1"
        }
        val context = RequestAuthorizationContext(request)

        authorizationManager.authorize({ TestingAuthenticationToken("any", null) }, context).isGranted shouldBe false
    }
}
