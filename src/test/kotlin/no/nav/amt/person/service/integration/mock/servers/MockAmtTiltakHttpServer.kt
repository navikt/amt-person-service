package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.amt_tiltak.BrukerInfoDto
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import java.util.UUID

class MockAmtTiltakHttpServer : MockHttpServer(name = "AmtTiltakHttpServer") {

	fun mockHentBrukerInfo(deltakerId: UUID, brukerInfo: BrukerInfoDto ) {
		val response = MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.toJsonString(brukerInfo))

		addResponseHandler("/api/tiltaksarrangor/deltaker/$deltakerId/bruker-info", response)
	}
}
