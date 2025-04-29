package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse

class MockNorgHttpServer : MockHttpServer(name = "MockNorgHttpServer") {

	private val baseUrl = "/norg2/api/v1/enhet"

	fun mockHentNavEnhet(navEnhet: NavEnhet) {
		addNavEnhet(navEnhet.enhetId, navEnhet.navn)
	}

	fun addNavAnsattEnhet() {
		addNavEnhet("0315", "Nav Grünerløkka")
	}

	fun addNavEnhet(enhetNr: String, navn: String) {
		val body = """
			{
				"navn": "$navn",
				"enhetNr": "$enhetNr"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)

		addResponseHandler("$baseUrl/${enhetNr}", response)
	}

}
