package no.nav.amt.person.service.api.auth

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.api.auth.MachineToMachineAuthorizationManager.Companion.isMachineToMachine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import java.time.Instant
import java.util.UUID

class MachineToMachineAuthorizationManagerTest {
    @Nested
    inner class AuthorizeTests {
        private val authorizationManager = MachineToMachineAuthorizationManager()

        @Test
        fun `authorize - principal er ikke jwt - returnerer false`() {
            val authentication = TestingAuthenticationToken("not-a-jwt", null)
            val context = RequestAuthorizationContext(MockHttpServletRequest())

            authorizationManager.authorize({ authentication }, context).isGranted shouldBe false
        }

        @Test
        fun `authorize - gyldig M2M jwt - returnerer true`() {
            val sub = UUID.randomUUID()
            val jwtAuthentication = JwtAuthenticationToken(createJwt(sub = sub, oid = sub))
            val context = RequestAuthorizationContext(MockHttpServletRequest())

            authorizationManager.authorize({ jwtAuthentication }, context).isGranted shouldBe true
        }

        @Test
        fun `authorize - gyldig jwt men ikke M2M - returnerer false`() {
            val jwtAuthentication = JwtAuthenticationToken(createJwt(sub = UUID.randomUUID(), oid = UUID.randomUUID()))
            val context = RequestAuthorizationContext(MockHttpServletRequest())

            authorizationManager.authorize({ jwtAuthentication }, context).isGranted shouldBe false
        }

        @Test
        fun `authorize - jwt uten oid - returnerer false`() {
            val jwtAuthentication = JwtAuthenticationToken(createJwt(sub = UUID.randomUUID(), oid = null))
            val context = RequestAuthorizationContext(MockHttpServletRequest())

            authorizationManager.authorize({ jwtAuthentication }, context).isGranted shouldBe false
        }
    }

    @Nested
    inner class IsMachineToMachineTests {
        @Test
        fun `isMachineToMachine - oid og sub er lik - er M2M token`() {
            val sub = UUID.randomUUID()
            val jwt = createJwt(sub = sub, oid = sub)

            isMachineToMachine(jwt) shouldBe true
        }

        @Test
        fun `isMachineToMachine - oid og sub er ikke lik - er ikke M2M token`() {
            val jwt = createJwt(sub = UUID.randomUUID(), oid = UUID.randomUUID())

            isMachineToMachine(jwt) shouldBe false
        }

        @Test
        fun `isMachineToMachine - oid mangler - returnerer false`() {
            val jwt = createJwt(sub = UUID.randomUUID(), oid = null)

            isMachineToMachine(jwt) shouldBe false
        }

        @Test
        fun `isMachineToMachine - sub mangler - returnerer false`() {
            val jwt = createJwt(sub = null, oid = UUID.randomUUID())

            isMachineToMachine(jwt) shouldBe false
        }
    }

    companion object {
        private fun createJwt(
            sub: UUID?,
            oid: UUID?,
        ): Jwt {
            val claims = mutableMapOf<String, Any>()
            if (sub != null) claims["sub"] = sub.toString()
            if (oid != null) claims["oid"] = oid.toString()

            return Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .claims { it.putAll(claims) }
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        }
    }
}
