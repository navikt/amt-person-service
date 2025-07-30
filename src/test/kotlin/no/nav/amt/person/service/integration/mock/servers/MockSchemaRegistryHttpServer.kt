package no.nav.amt.person.service.integration.mock.servers

import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.apache.avro.Schema

class MockSchemaRegistryHttpServer : MockHttpServer(name = "MockSchemaRegistryHttpServer") {
	fun registerSchema(
		id: Int,
		topic: String,
		schema: Schema,
	) {
		val schemaString = SchemaString(schema.toString()).toJson()

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/subjects/$topic-value/versions?normalize=false" &&
				req.method == "POST"
		}

		addResponseHandler(requestPredicate, MockResponse().setResponseCode(200).setBody("""{"id":"$id"}"""))

		addResponseHandler(
			"/schemas/ids/$id?fetchMaxId=false&subject=$topic-value",
			MockResponse().setResponseCode(200).setBody(schemaString),
		)
	}
}
