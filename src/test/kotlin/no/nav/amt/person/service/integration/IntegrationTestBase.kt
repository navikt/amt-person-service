package no.nav.amt.person.service.integration

import no.nav.amt.person.service.integration.mock.servers.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration

@ActiveProfiles("integration")
@TestConfiguration("application-integration.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTestBase {

	@LocalServerPort
	private var port: Int = 0

	val client = OkHttpClient.Builder()
		.callTimeout(Duration.ofMinutes(5))
		.readTimeout(Duration.ofMinutes(5))
		.build()

	companion object {
		val mockPdlHttpServer = MockPdlHttpServer()
		val mockMachineToMachineHttpServer = MockMachineToMachineHttpServer()
		val mockKrrProxyHttpServer = MockKrrProxyHttpServer()
		val mockVeilarboppfolgingHttpServer = MockVeilarboppfolgingHttpServer()
		val mockNorgHttpServer = MockNorgHttpServer()
		val mockPoaoTilgangHttpServer = MockPoaoTilgangHttpServer()
		val mockNomHttpServer = MockNomHttpServer()

		@JvmStatic
		@DynamicPropertySource
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			mockPdlHttpServer.start()
			registry.add("pdl.url") { mockPdlHttpServer.serverUrl() }
			registry.add("pdl.scope") { "test.pdl" }

			mockMachineToMachineHttpServer.start()
			registry.add("nais.env.azureOpenIdConfigTokenEndpoint") {
				mockMachineToMachineHttpServer.serverUrl() + MockMachineToMachineHttpServer.tokenPath
			}

			mockKrrProxyHttpServer.start()
			registry.add("digdir-krr-proxy.url") { mockKrrProxyHttpServer.serverUrl() }
			registry.add("digdir-krr-proxy.scope") { "test.digdir-krr-proxy" }

			mockVeilarboppfolgingHttpServer.start()
			registry.add("veilarboppfolging.url") { mockVeilarboppfolgingHttpServer.serverUrl() }
			registry.add("veilarboppfolging.scope") { "test.veilarboppfolging" }

			mockNorgHttpServer.start()
			registry.add("norg.url") { mockNorgHttpServer.serverUrl() }

			mockPoaoTilgangHttpServer.start()
			registry.add("poao-tilgang.url") { mockPoaoTilgangHttpServer.serverUrl() }
			registry.add("poao-tilgang.scope") { "test.poao-tilgang" }

			mockNomHttpServer.start()
			registry.add("nom.url") { mockNomHttpServer.serverUrl() }
			registry.add("nom.scope") { "test.nom" }
		}

	}

	fun serverUrl() = "http://localhost:$port"

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap()
	): Response {
		val reqBuilder = Request.Builder()
			.url("${serverUrl()}$path")
			.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}
}
