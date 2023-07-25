package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EndringPaaBrukerIngestorTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Test
	fun `ingest - bruker finnes, har ikke nav kontor - oppretter og oppdaterer nav kontor`() {
		val navEnhet = TestData.lagNavEnhet()
		val navBruker = TestData.lagNavBruker(navEnhet = null)

		val msg = KafkaMessageCreator.lagEndringPaaBrukerMsg(
			fodselsnummer = navBruker.person.personident,
			oppfolgingsenhet = navEnhet.enhetId,
		)

		testDataRepository.insertNavBruker(navBruker)

		mockNorgHttpServer.addNavEnhet(navEnhet.enhetId, navEnhet.navn)
		kafkaMessageSender.sendTilEndringPaaBrukerTopic(msg.toJson())


		AsyncUtils.eventually {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.navEnhet!!.enhetId shouldBe navEnhet.enhetId
			faktiskBruker.navEnhet!!.navn shouldBe navEnhet.navn

		}
	}

}
