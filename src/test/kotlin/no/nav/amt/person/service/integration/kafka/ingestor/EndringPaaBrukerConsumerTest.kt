package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.NavBrukerService
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class EndringPaaBrukerConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerService: NavBrukerService,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes, har ikke nav kontor - oppretter og oppdaterer nav kontor`() {
		val navEnhet = TestData.lagNavEnhet()
		val navBruker = TestData.lagNavBruker(navEnhet = null)

		val msg =
			KafkaMessageCreator.lagEndringPaaBrukerMsg(
				fodselsnummer = navBruker.person.personident,
				oppfolgingsenhet = navEnhet.enhetId,
			)

		testDataRepository.insertNavBruker(navBruker)

		mockNorgHttpServer.addNavEnhet(navEnhet.enhetId, navEnhet.navn)
		kafkaMessageSender.sendTilEndringPaaBrukerTopic(msg.toJson())

		await().untilAsserted {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			assertSoftly(faktiskBruker.navEnhet.shouldNotBeNull()) {
				enhetId shouldBe navEnhet.enhetId
				navn shouldBe navEnhet.navn
			}
		}
	}
}
