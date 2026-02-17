package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class SkjermetPersonConsumerTest(
	private val navBrukerRepository: NavBrukerRepository,
	private val kafkaMessageSender: KafkaMessageSender,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes - skal oppdatere med skjermingsdata`() {
		val bruker = TestData.lagNavBruker(erSkjermet = false)
		testDataRepository.insertNavBruker(bruker)

		kafkaMessageSender.sendTilSkjermetPersonTopic(bruker.person.personident, true)

		await().untilAsserted {
			val faktiskBruker = navBrukerRepository.get(bruker.id)
			faktiskBruker.erSkjermet shouldBe true
		}
	}
}
