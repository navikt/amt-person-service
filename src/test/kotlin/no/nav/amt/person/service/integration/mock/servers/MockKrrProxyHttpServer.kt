package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse

class MockKrrProxyHttpServer : MockHttpServer(name = "MockKrrProxyHttpServer") {

	fun mockHentKontaktinformasjon(kontaktinformasjon: MockKontaktinformasjon) {
		val response = MockResponse()
				.setResponseCode(200)
				.setBody(toJsonString(kontaktinformasjon))
		addResponseHandler("/rest/v1/person?inkluderSikkerDigitalPost=false", response)
	}
}

data class MockKontaktinformasjon(
	val personident: String,
	val epostadresse: String?,
	val mobiltelefonnummer: String?,
)

