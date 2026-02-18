package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class MockPoaoTilgangHttpServer : MockHttpServer(name = "MockPoaoTilgangHttpServer") {
	fun addErSkjermetResponse(data: Map<String, Boolean>) {
		val url = "/api/v1/skjermet-person"

		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url &&
				req.method == HttpMethod.POST.name() &&
				data.keys.map { body.contains(it) }.all { true }
		}

		val response =
			MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody(staticObjectMapper.writeValueAsString(data))

		addResponseHandler(predicate, response)
	}
}
