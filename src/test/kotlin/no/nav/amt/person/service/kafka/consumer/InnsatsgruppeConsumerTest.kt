package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class InnsatsgruppeConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerRepository: NavBrukerRepository,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes, ny innsatsgruppe - oppdaterer`() {
		val navBruker = TestData.lagNavBruker(innsatsgruppe = InnsatsgruppeV1.STANDARD_INNSATS)
		testDataRepository.insertNavBruker(navBruker)

		val siste14aVedtak =
			InnsatsgruppeConsumer.Siste14aVedtak(
				aktorId = navBruker.person.personident,
				innsatsgruppe = InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS,
			)

		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, navBruker.person.personident)
		kafkaMessageSender.sendTilInnsatsgruppeTopic(siste14aVedtak)

		await().untilAsserted {
			val faktiskBruker = navBrukerRepository.get(navBruker.id)

			faktiskBruker.innsatsgruppe shouldBe InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		val siste14aVedtak =
			InnsatsgruppeConsumer.Siste14aVedtak(
				aktorId = "1234",
				innsatsgruppe = InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS,
			)
		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilInnsatsgruppeTopic(siste14aVedtak)

		withLogCapture(InnsatsgruppeConsumer::class.java.name) { loggingEvents ->
			await().untilAsserted {
				loggingEvents.any {
					it.message == "Innsatsgruppe endret. Nav-bruker finnes ikke, hopper over Kafka-melding"
				} shouldBe true
			}
		}
	}
}
