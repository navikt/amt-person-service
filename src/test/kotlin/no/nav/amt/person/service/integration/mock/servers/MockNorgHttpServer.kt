package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse

class MockNorgHttpServer : MockHttpServer(name = "MockNorgHttpServer") {

	private val baseUrl = "/norg2/api/v1/enhet"

	fun addDefaultData() {
		addNavEnhet("INTEGRATION_TEST_NAV_ENHET", "INTEGRATION_TEST_NAV_ENHET_NAVN")
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
