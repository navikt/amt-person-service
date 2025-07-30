package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockPoaoTilgangHttpServer : MockHttpServer(name = "MockPoaoTilgangHttpServer") {
	fun addErSkjermetResponse(data: Map<String, Boolean>) {
		val url = "/api/v1/skjermet-person"

		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url &&
				req.method == "POST" &&
				data.keys.map { body.contains(it) }.all { true }
		}

		val response =
			MockResponse()
				.setResponseCode(200)
				.setBody(toJsonString(data))

		addResponseHandler(predicate, response)
	}
}
