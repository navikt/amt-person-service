package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SkjermetPersonConsumerTest: IntegrationTestBase() {

	@Autowired
	private lateinit var navBrukerService: NavBrukerService

	@Autowired
	private lateinit var kafkaMessageSender: KafkaMessageSender

	@Test
	fun `ingest - bruker finnes - skal oppdatere med skjermingsdata`() {
		val bruker = TestData.lagNavBruker(erSkjermet = false)
		testDataRepository.insertNavBruker(bruker)

		kafkaMessageSender.sendTilSkjermetPersonTopic(bruker.person.personident, true)

		AsyncUtils.eventually {
			val faktiskBruker = navBrukerService.hentNavBruker(bruker.id)
			faktiskBruker.erSkjermet shouldBe true
		}

	}

}
