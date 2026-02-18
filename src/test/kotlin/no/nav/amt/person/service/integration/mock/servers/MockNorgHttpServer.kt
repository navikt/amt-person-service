package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.norg.NorgNavEnhetDto
import no.nav.amt.person.service.data.TestData.navGrunerlokka
import no.nav.amt.person.service.navenhet.NavEnhetDbo
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import org.springframework.http.HttpStatus

class MockNorgHttpServer : MockHttpServer(name = "MockNorgHttpServer") {
	companion object {
		private const val BASE_URL = "/norg2/api/v1/enhet"
	}

	fun addNavEnhetGrunerLokka() {
		addNavEnhet(navGrunerlokka)
	}

	fun addNavEnhet(navEnhet: NavEnhetDbo) {
		val response =
			MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody(
					staticObjectMapper.writeValueAsString(NorgNavEnhetDto.fromDbo(navEnhet)),
				)

		addResponseHandler("$BASE_URL/${navEnhet.enhetId}", response)
	}
}
