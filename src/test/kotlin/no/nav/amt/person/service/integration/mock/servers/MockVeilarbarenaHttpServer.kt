package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpStatus

class MockVeilarbarenaHttpServer : MockHttpServer(name = "MockVeilarbarenaHttpServer") {
	fun mockHentBrukerOppfolgingsenhetId(
		fnr: String,
		oppfolgingsenhet: String?,
	) {
		val url = "/veilarbarena/api/v2/arena/hent-status"
		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url &&
				req.method == "POST" &&
				body.contains(fnr)
		}

		val enhet = if (oppfolgingsenhet == null) "null" else "\"$oppfolgingsenhet\""
		val response =
			MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody("""{"oppfolgingsenhet": $enhet}""")

		addResponseHandler(predicate, response)
	}
}
