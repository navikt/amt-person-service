package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockSchemaRegistryHttpServer : MockHttpServer(name = "MockSchemaRegistryHttpServer") {


	fun registerSchema(id: Int, topic: String, schema: String) {
		val requestPredicate = { req: RecordedRequest ->
			req.path == "/subjects/$topic-value/versions?normalize=false"
				&& req.method == "POST"
		}

		addResponseHandler(requestPredicate, MockResponse().setResponseCode(200).setBody("""{"id":"$id"}"""))

		addResponseHandler("/schemas/ids/${id}?fetchMaxId=false&subject=$topic-value", MockResponse().setResponseCode(200).setBody(schema))

	}

}
