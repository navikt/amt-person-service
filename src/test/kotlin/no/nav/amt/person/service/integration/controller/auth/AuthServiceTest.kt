package no.nav.amt.person.service.integration.controller.auth

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.amt.person.service.api.auth.AuthService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

class AuthServiceTest {
    private val authService = AuthService()

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `verifyRequestIsMachineToMachine - oid og sub er lik - er M2M token`() {
        val sub = UUID.randomUUID().toString()
        setSecurityContext(sub = sub, oid = sub)

        shouldNotThrowAny {
            authService.verifyRequestIsMachineToMachine()
        }
    }

    @Test
    fun `verifyRequestIsMachineToMachine - oid og sub er ikke lik - er ikke M2M token`() {
        val sub = UUID.randomUUID().toString()
        setSecurityContext(sub = sub, oid = UUID.randomUUID().toString())

        shouldThrow<AccessDeniedException> {
            authService.verifyRequestIsMachineToMachine()
        }
    }

    @Test
    fun `verifyRequestIsMachineToMachine - oid mangler - er ikke M2M token`() {
        val sub = UUID.randomUUID().toString()
        setSecurityContext(sub = sub, oid = null)

        shouldThrow<AccessDeniedException> {
            authService.verifyRequestIsMachineToMachine()
        }
    }

    private fun setSecurityContext(
        sub: String,
        oid: String?,
    ) {
        val claims = mutableMapOf(
            "sub" to sub,
            "iss" to "http://localhost:9999/azuread",
            "aud" to listOf("test-aud"),
        )
        if (oid != null) claims["oid"] = oid

        val jwt = Jwt
            .withTokenValue("test-token")
            .header("alg", "RS256")
            .claims { it.putAll(claims) }
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }
}
