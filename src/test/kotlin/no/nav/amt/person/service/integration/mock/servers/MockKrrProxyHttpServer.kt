package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse

class MockKrrProxyHttpServer : MockHttpServer(name = "MockKrrProxyHttpServer") {
	fun mockHentKontaktinformasjon(kontaktinformasjon: MockKontaktinformasjon) {
		val response =
			MockResponse()
				.setResponseCode(200)
				.setBody(
					staticObjectMapper.writeValueAsString(MockKResponse(mapOf(kontaktinformasjon.personident to kontaktinformasjon))),
				)
		addResponseHandler("/rest/v1/personer?inkluderSikkerDigitalPost=false", response)
	}
}

data class MockKResponse(
	val personer: Map<String, MockKontaktinformasjon>,
	val feil: Map<String, String> = emptyMap(),
)

data class MockKontaktinformasjon(
	val personident: String,
	val epostadresse: String?,
	val mobiltelefonnummer: String?,
)
