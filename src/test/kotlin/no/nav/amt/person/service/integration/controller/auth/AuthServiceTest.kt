package no.nav.amt.person.service.integration.controller.auth

import no.nav.amt.person.service.controller.auth.AuthService
import no.nav.amt.person.service.controller.auth.Issuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AuthServiceTest {
	companion object {
		private val server = MockOAuth2Server()

		init {
			server.start()
		}

		@AfterAll
		@JvmStatic
		fun cleanup() {
			server.shutdown()
		}
	}

	@Test
	fun `verifyRequestIsMachineToMachine - oid og sub er lik - er M2M token`() {
		val sub = UUID.randomUUID().toString()
		val claims = mutableMapOf<String, Any>()
		claims["roles"] = arrayOf("access_as_application")
		claims["oid"] = sub

		val token = server.issueToken(
			issuerId = Issuer.AZURE_AD,
			subject = sub,
			audience = "test-aud",
			claims = claims
		).serialize()

		val authService = AuthService(mockContextHolder(Issuer.AZURE_AD, token))

		assertDoesNotThrow {
			authService.verifyRequestIsMachineToMachine()
		}
	}

	@Test
	fun `verifyRequestIsMachineToMachine - oid og sub er ikke lik - er ikke M2M token`() {
		val sub = UUID.randomUUID().toString()
		val claims = mutableMapOf<String, Any>()
		claims["roles"] = arrayOf("access_as_application")
		claims["oid"] = UUID.randomUUID()

		val token = server.issueToken(
			issuerId = Issuer.AZURE_AD,
			subject = sub,
			audience = "test-aud",
			claims = claims
		).serialize()

		val authService = AuthService(mockContextHolder(Issuer.AZURE_AD, token))

		assertThrows<JwtTokenUnauthorizedException> {
			authService.verifyRequestIsMachineToMachine()
		}
	}

	@Test
	fun `verifyRequestIsMachineToMachine - oid mangler - er ikke M2M token`() {
		val sub = UUID.randomUUID().toString()
		val claims = mutableMapOf<String, Any>()

		val token = server.issueToken(
			issuerId = Issuer.AZURE_AD,
			subject = sub,
			audience = "test-aud",
			claims = claims
		).serialize()

		val authService = AuthService(mockContextHolder(Issuer.AZURE_AD, token))

		assertThrows<JwtTokenUnauthorizedException> {
			authService.verifyRequestIsMachineToMachine()
		}
	}

	private fun mockContextHolder(issuer: String, token: String) : TokenValidationContextHolder {
		return object: TokenValidationContextHolder {
			override fun getTokenValidationContext(): TokenValidationContext {
				return TokenValidationContext(
					mapOf(
						issuer to JwtToken(token)
					)
				)
			}
			override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) {
				throw NotImplementedError()
			}
		}
	}

}
