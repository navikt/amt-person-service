package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse

class MockVeilarbarenaHttpServer : MockHttpServer(name = "MockVeilarbarenaHttpServer") {

	fun mockHentBrukerOppfolgingsenhetId(fnr: String, oppfolgingsenhet: String?) {
		val enhet = if (oppfolgingsenhet == null) "null" else "\"$oppfolgingsenhet\""
		val response = MockResponse()
				.setResponseCode(200)
				.setBody("""{"oppfolgingsenhet": $enhet}""")

		addResponseHandler("/veilarbarena/api/arena/status?fnr=$fnr", response)
	}
}
