package no.nav.amt.person.service.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import no.nav.amt.person.service.clients.KodeverkClient
import no.nav.amt.person.service.clients.VeilarboppfolgingClient
import no.nav.amt.person.service.clients.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.oppfolgingskontor.OppfolgingskontorClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.data.RepositoryTestBase
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.ObjectMapper

@ActiveProfiles("integration")
@EnableMockOAuth2Server
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase : RepositoryTestBase() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var krrProxyClient: KrrProxyClient

    @MockkBean
    lateinit var nomClient: NomClient

    @MockkBean
    lateinit var norgClient: NorgClient

    @MockkBean
    lateinit var oppfolgingskontorClient: OppfolgingskontorClient

    @MockkBean
    lateinit var pdlClient: PdlClient

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    lateinit var poaoTilgangClient: PoaoTilgangClient

    @MockkBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

    @MockkBean
    lateinit var veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient

    @MockkBean
    lateinit var kafkaProducerClient: KafkaProducerClient<String, String>

    @BeforeEach
    fun setup() {
        every { kafkaProducerClient.sendSync(any()) } returns null
    }

    @AfterEach
    fun cleanUp() {
        clearMocks(
            krrProxyClient,
            nomClient,
            norgClient,
            oppfolgingskontorClient,
            pdlClient,
            kodeverkClient,
            poaoTilgangClient,
            veilarboppfolgingClient,
            veilarbvedtaksstotteClient,
            kafkaProducerClient,
        )
    }
}
