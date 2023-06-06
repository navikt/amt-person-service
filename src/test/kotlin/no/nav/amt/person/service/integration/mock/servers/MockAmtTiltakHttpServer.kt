package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.amt_tiltak.BrukerInfoDto
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.UUID

class MockAmtTiltakHttpServer : MockHttpServer(name = "AmtTiltakHttpServer") {

	fun mockHentBrukerInfo(deltakerId: UUID, brukerInfo: BrukerInfoDto ) {
		val response = MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.toJsonString(brukerInfo))

		addResponseHandler("/api/tiltaksarrangor/deltaker/$deltakerId/bruker-info", response)
	}

	fun mockHentBrukerId(personIdent: String, brukerInfo: BrukerInfoDto?) {
		val response = if (brukerInfo == null) {
			MockResponse()
				.setResponseCode(404)
		} else {
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.toJsonString(brukerInfo))
		}


		val requestPredicate = { req: RecordedRequest ->
			req.path == "/api/tiltaksarrangor/deltaker/bruker-info"
				&& req.method == "POST"
				&& req.getBodyAsString() == """{"personident": "$personIdent"}"""
		}

		addResponseHandler(requestPredicate, response)
	}

}
