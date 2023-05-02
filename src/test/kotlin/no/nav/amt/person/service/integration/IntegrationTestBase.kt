package no.nav.amt.person.service.integration

import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.integration.kafka.utils.SingletonKafkaProvider
import no.nav.amt.person.service.integration.mock.servers.*
import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.AfterClass
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
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
class IntegrationTestBase {

	@LocalServerPort
	private var port: Int = 0

	val client = OkHttpClient.Builder()
		.callTimeout(Duration.ofMinutes(5))
		.readTimeout(Duration.ofMinutes(5))
		.build()

	@Autowired
	lateinit var testDataRepository: TestDataRepository

	@AfterEach
	fun cleanUp() {
		mockKrrProxyHttpServer.resetHttpServer()
		mockNomHttpServer.resetHttpServer()
		mockNorgHttpServer.resetHttpServer()
		mockPdlHttpServer.resetHttpServer()
		mockPoaoTilgangHttpServer.resetHttpServer()
		mockVeilarbarenaHttpServer.resetHttpServer()
		mockVeilarboppfolgingHttpServer.resetHttpServer()
	}

	companion object {
		val mockPdlHttpServer = MockPdlHttpServer()
		val mockMachineToMachineHttpServer = MockMachineToMachineHttpServer()
		val mockKrrProxyHttpServer = MockKrrProxyHttpServer()
		val mockVeilarboppfolgingHttpServer = MockVeilarboppfolgingHttpServer()
		val mockNorgHttpServer = MockNorgHttpServer()
		val mockPoaoTilgangHttpServer = MockPoaoTilgangHttpServer()
		val mockNomHttpServer = MockNomHttpServer()
		val mockVeilarbarenaHttpServer = MockVeilarbarenaHttpServer()
		val mockSchemaRegistryHttpServer = MockSchemaRegistryHttpServer()

		val dataSource = SingletonPostgresContainer.getDataSource()

		@JvmStatic
		@DynamicPropertySource
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			mockSchemaRegistryHttpServer.start()
			registry.add("kafka.schema.registry.url") { mockSchemaRegistryHttpServer.serverUrl() }

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

			mockVeilarbarenaHttpServer.start()
			registry.add("veilarbarena.url") { mockVeilarbarenaHttpServer.serverUrl() }
			registry.add("veilarbarena.scope") { "test.veilarbarena" }

			val container = SingletonPostgresContainer.getContainer()

			registry.add("spring.datasource.url") { container.jdbcUrl }
			registry.add("spring.datasource.username") { container.username }
			registry.add("spring.datasource.password") { container.password }
			registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }
		}

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
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

@Profile("integration")
@TestConfiguration
class IntegrationTestConfiguration {

	@Bean
	fun kafkaProperties(): KafkaProperties {
		return SingletonKafkaProvider.getKafkaProperties()
	}

}
