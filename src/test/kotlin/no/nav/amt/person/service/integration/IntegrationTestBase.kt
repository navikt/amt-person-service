package no.nav.amt.person.service.integration

import no.nav.amt.person.service.data.RepositoryTestBase
import no.nav.amt.person.service.integration.kafka.utils.SingletonKafkaProvider
import no.nav.amt.person.service.integration.mock.servers.MockKrrProxyHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockMachineToMachineHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockNomHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockNorgHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockOAuthServer
import no.nav.amt.person.service.integration.mock.servers.MockPdlHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockPoaoTilgangHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockSchemaRegistryHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockVeilarbarenaHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockVeilarboppfolgingHttpServer
import no.nav.amt.person.service.integration.mock.servers.MockVeilarbvedtaksstotteHttpServer
import no.nav.amt.person.service.kafka.config.KafkaProperties
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration

@ActiveProfiles("integration")
@Import(IntegrationTestConfiguration::class)
@TestConfiguration("application-integration.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase : RepositoryTestBase() {
	@LocalServerPort
	private var port: Int = 0

	val client =
		OkHttpClient
			.Builder()
			.callTimeout(Duration.ofMinutes(5))
			.readTimeout(Duration.ofMinutes(5))
			.build()

	@AfterEach
	fun cleanUp() {
		mockKrrProxyHttpServer.resetHttpServer()
		mockNomHttpServer.resetHttpServer()
		mockNorgHttpServer.resetHttpServer()
		mockPdlHttpServer.resetHttpServer()
		mockPoaoTilgangHttpServer.resetHttpServer()
		mockVeilarbarenaHttpServer.resetHttpServer()
		mockVeilarboppfolgingHttpServer.resetHttpServer()
		mockVeilarboppfolgingHttpServer.resetHttpServer()
	}

	companion object {
		val mockPdlHttpServer = MockPdlHttpServer()
		val mockMachineToMachineHttpServer = MockMachineToMachineHttpServer()
		val mockKrrProxyHttpServer = MockKrrProxyHttpServer()
		val mockVeilarboppfolgingHttpServer = MockVeilarboppfolgingHttpServer()
		val mockVeilarbvedtaksstotteHttpServer = MockVeilarbvedtaksstotteHttpServer()
		val mockNorgHttpServer = MockNorgHttpServer()
		val mockPoaoTilgangHttpServer = MockPoaoTilgangHttpServer()
		val mockNomHttpServer = MockNomHttpServer()
		val mockVeilarbarenaHttpServer = MockVeilarbarenaHttpServer()
		val mockSchemaRegistryHttpServer = MockSchemaRegistryHttpServer()
		val mockOAuthServer = MockOAuthServer()

		@JvmStatic
		@DynamicPropertySource
		@Suppress("unused")
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			mockSchemaRegistryHttpServer.start()
			registry.add("kafka.schema.registry.url") { mockSchemaRegistryHttpServer.serverUrl() }

			mockPdlHttpServer.start()
			registry.add("pdl.url") { mockPdlHttpServer.serverUrl() }
			registry.add("pdl.scope") { "test.pdl" }

			mockMachineToMachineHttpServer.start()
			registry.add("nais.env.azureOpenIdConfigTokenEndpoint") {
				mockMachineToMachineHttpServer.serverUrl() + MockMachineToMachineHttpServer.TOKEN_PATH
			}

			mockKrrProxyHttpServer.start()
			registry.add("digdir-krr-proxy.url") { mockKrrProxyHttpServer.serverUrl() }
			registry.add("digdir-krr-proxy.scope") { "test.digdir-krr-proxy" }

			mockVeilarboppfolgingHttpServer.start()
			registry.add("veilarboppfolging.url") { mockVeilarboppfolgingHttpServer.serverUrl() }
			registry.add("veilarboppfolging.scope") { "test.veilarboppfolging" }

			mockVeilarbvedtaksstotteHttpServer.start()
			registry.add("veilarbvedtaksstotte.url") { mockVeilarbvedtaksstotteHttpServer.serverUrl() }
			registry.add("veilarbvedtaksstotte.scope") { "test.veilarbvedtaksstotte" }

			mockNorgHttpServer.start()
			registry.add("norg.url") { mockNorgHttpServer.serverUrl() }

			mockPoaoTilgangHttpServer.start()
			registry.add("poao-tilgang.url") { mockPoaoTilgangHttpServer.serverUrl() }
			registry.add("poao-tilgang.scope") { "test.poao-tilgang" }

			mockNomHttpServer.start()
			registry.add("nom.url") { mockNomHttpServer.serverUrl() }
			registry.add("nom.scope") { "test.nom" }

			mockVeilarbarenaHttpServer.start()
			registry.add("veilarbarena.url") { mockVeilarbarenaHttpServer.serverUrl() }
			registry.add("veilarbarena.scope") { "test.veilarbarena" }

			mockOAuthServer.start()
			registry.add("no.nav.security.jwt.issuer.azuread.discovery-url") { mockOAuthServer.getDiscoveryUrl("azuread") }
			registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test-aud" }

			registry.add("kodeverk.url") { "http://kodeverk" }
			registry.add("kodeverk.scope") { "test.kodeverk" }
		}
	}

	fun serverUrl() = "http://localhost:$port"

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap(),
	): Response {
		val reqBuilder =
			Request
				.Builder()
				.url("${serverUrl()}$path")
				.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}

	fun String.toJsonRequestBody(): RequestBody {
		val mediaTypeJson = "application/json".toMediaType()
		return this.toRequestBody(mediaTypeJson)
	}

	fun emptyRequest(): RequestBody {
		val mediaTypeHtml = "application/json".toMediaType()
		return "".toRequestBody(mediaTypeHtml)
	}
}

@Profile("integration")
@TestConfiguration
class IntegrationTestConfiguration {
	@Bean
	fun kafkaProperties(): KafkaProperties = SingletonKafkaProvider.getKafkaProperties()
}
