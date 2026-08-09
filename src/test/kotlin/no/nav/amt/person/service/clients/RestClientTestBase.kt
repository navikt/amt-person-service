package no.nav.amt.person.service.clients

import no.nav.amt.person.service.config.ClientConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.client.MockRestServiceServer

@Import(ClientTestConfig::class, ClientConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
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
