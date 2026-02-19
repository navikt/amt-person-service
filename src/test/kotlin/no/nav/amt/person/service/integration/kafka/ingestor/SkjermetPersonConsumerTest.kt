package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.kafka.consumer.SkjermetPersonConsumer
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class SkjermetPersonConsumerTest(
	private val navBrukerRepository: NavBrukerRepository,
	private val kafkaMessageSender: KafkaMessageSender,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes - skal oppdatere med skjermingsdata`() {
		val navBruker = TestData.lagNavBruker(erSkjermet = false)
		testDataRepository.insertNavBruker(navBruker)

		kafkaMessageSender.sendTilSkjermetPersonTopic(navBruker.person.personident, true)

		await().untilAsserted {
			val faktiskBruker = navBrukerRepository.get(navBruker.id)
			faktiskBruker.erSkjermet shouldBe true
		}
	}

	@Test
	fun `ingest tombstone - bruker finnes - skal kaste exception og logge feilmelding`() {
		val navBruker = TestData.lagNavBruker(erSkjermet = false)
		testDataRepository.insertNavBruker(navBruker)

		kafkaMessageSender.sendTilSkjermetPersonTopic(navBruker.person.personident, null)

		withLogCapture(SkjermetPersonConsumer::class.java.name) { loggingEvents ->
			await().untilAsserted {
				loggingEvents.any {
					it.message.contains("Kan ikke ingeste tombstone for eksisterende Nav-bruker ${navBruker.id}")
				} shouldBe true
			}
		}
	}
}
