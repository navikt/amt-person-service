package no.nav.amt.person.service.integration.mock.servers

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory
import java.util.UUID

open class MockOAuthServer {

	private val azureAdIssuer = "azuread"

	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private val server = MockOAuth2Server()
	}

	fun start() {
		try {
			server.start()
		} catch (e: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	fun getDiscoveryUrl(issuer: String = azureAdIssuer): String {
		return server.wellKnownUrl(issuer).toString()
	}

	fun issueToken(
		issuer: String,
		subject: String = UUID.randomUUID().toString(),
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		return server.issueToken(issuer, subject, audience, claims).serialize()
	}

	fun issueAzureAdM2MToken(
		subject: String = UUID.randomUUID().toString(),
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		val claims = claims.toMutableMap()
		claims["roles"] = arrayOf("access_as_application")
		claims["oid"] = subject

		return server.issueToken(azureAdIssuer, subject, audience, claims).serialize()
	}

}
