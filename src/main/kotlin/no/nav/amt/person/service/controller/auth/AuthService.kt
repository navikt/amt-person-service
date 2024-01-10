package no.nav.amt.person.service.controller.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
	private val tokenValidationContextHolder: TokenValidationContextHolder
) {

	fun verifyRequestIsMachineToMachine() {
		if (!isRequestFromMachine()) {
			throw JwtTokenUnauthorizedException("Request is not machine-to-machine")
		}
	}

	private fun isRequestFromMachine(): Boolean {
		val claims = tokenValidationContextHolder
			.getTokenValidationContext()
			.getClaims(Issuer.AZURE_AD)

		val sub = claims.getStringClaim("sub")?.let { UUID.fromString(it) }
			?: throw JwtTokenUnauthorizedException("Sub is missing")
		val oid = claims.getStringClaim("oid")?.let { UUID.fromString(it) }
			?: throw JwtTokenUnauthorizedException("Oid is missing")

		return sub == oid
	}

}
