package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class SisteOppfolgingsperiodeConsumerTest(
    private val kafkaMessageSender: KafkaMessageSender,
    private val navBrukerRepository: NavBrukerRepository,
) : IntegrationTestBase() {
    @Test
    fun `ingest - bruker finnes, har ikke Nav-kontor - oppretter og oppdaterer Nav-kontor`() {
        val navEnhet = TestData.lagNavEnhet()
        val navBruker = TestData.lagNavBruker(navEnhet = null)

        val kafkaPayload =
            KafkaMessageCreator.lagSisteOppfolgingsperiodeMsg(
                ident = navBruker.person.personident,
                kontorId = navEnhet.enhetId,
                kontorNavn = navEnhet.navn,
            )

        testDataRepository.insertNavBruker(navBruker)

        mockNorgHttpServer.addNavEnhet(navEnhet)
        kafkaMessageSender.sendTilSisteOppfolgingsperiodeTopic(kafkaPayload)

        await().untilAsserted {
            val faktiskBruker = navBrukerRepository.get(navBruker.id)

            assertSoftly(faktiskBruker.navEnhet.shouldNotBeNull()) {
                enhetId shouldBe navEnhet.enhetId
                navn shouldBe navEnhet.navn
            }
        }
    }
}
