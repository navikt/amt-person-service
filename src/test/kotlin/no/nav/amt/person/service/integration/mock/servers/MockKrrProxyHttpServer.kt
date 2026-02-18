package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.krr.PostPersonerResponse
import no.nav.amt.person.service.navbruker.NavBrukerDbo
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import org.springframework.http.HttpStatus

class MockKrrProxyHttpServer : MockHttpServer(name = "MockKrrProxyHttpServer") {
	fun mockHentKontaktinformasjon(navBruker: NavBrukerDbo) {
		val response =
			MockResponse()
				.setResponseCode(HttpStatus.OK.value()) //
				.setBody(
					staticObjectMapper.writeValueAsString(
						PostPersonerResponse(
							mapOf(
								navBruker.person.personident to
									PostPersonerResponse.KontaktinformasjonDto(
										personident = navBruker.person.personident,
										epostadresse = navBruker.epost,
										mobiltelefonnummer = navBruker.telefon,
									),
							),
							feil = emptyMap(),
						),
					),
				)

		addResponseHandler("/rest/v1/personer?inkluderSikkerDigitalPost=false", response)
	}
}
