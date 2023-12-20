package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockVeilarboppfolgingHttpServer : MockHttpServer(name = "MockVeilarboppfolgingHttpServer") {

	fun mockHentVeilederIdent(fnr: String, veilederIdent: String?) {
		val url = "/veilarboppfolging/api/v3/hent-veileder"
		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url
				&& req.method == "POST"
				&& body.contains(fnr)
		}

		val enhet = if (veilederIdent == null) "null" else "\"$veilederIdent\""
		val response = if (veilederIdent == null) {
			MockResponse().setResponseCode(204)
		} else {
			MockResponse()
				.setResponseCode(200)
				.setBody("""{"veilederIdent": "$veilederIdent"}""")
		}
		addResponseHandler(predicate, response)
	}
}
