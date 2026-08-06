package no.nav.amt.person.service.clients

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer

@Import(ClientTestConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestPropertySource(
    properties = [
        "spring.http.serviceclient.pdl-api.base-url=http://localhost:8080",
        "spring.http.serviceclient.digdir-krr-proxy.base-url=http://localhost:8080",
        "spring.http.serviceclient.veilarboppfolging.base-url=http://localhost:8080",
        "spring.http.serviceclient.veilarbvedtaksstotte.base-url=http://localhost:8080",
        "spring.http.serviceclient.kodeverk-api.base-url=http://localhost:8080",
        "spring.http.serviceclient.nom-api.base-url=http://localhost:8080",
        "spring.http.serviceclient.ao-oppfolgingskontor.base-url=http://localhost:8080",
        "spring.test.restclient.mockrestserviceserver.enabled=false",
    ],
)
abstract class RestClientTestBase(
    private val group: String,
) {
    @Autowired
    private lateinit var testConfig: ClientTestConfig

    lateinit var server: MockRestServiceServer

    @BeforeEach
    fun resetServer() {
        server = testConfig.getMock(group)
        server.reset()
    }

    companion object {
        const val TOKEN_IN_TEST = ClientTestConfig.TOKEN
    }
}
