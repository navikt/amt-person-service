package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.kafka.consumer.TildeltVeilederConsumer
import no.nav.amt.person.service.navansatt.NavAnsattRepository
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class TildeltVeilederConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerRepository: NavBrukerRepository,
	private val navAnsattRepository: NavAnsattRepository,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes, ny veileder - oppretter og oppdaterer nav veileder`() {
		val navBruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(navBruker)

		val msg = KafkaMessageCreator.lagTildeltVeilederMsg()
		val navAnsatt = TestData.lagNavAnsatt(navIdent = msg.veilederId)

		mockPdlHttpServer.mockHentIdenter(msg.aktorId, navBruker.person.personident)
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt)
		mockNorgHttpServer.addNavEnhetGrunerLokka()

		kafkaMessageSender.sendTilTildeltVeilederTopic(msg.toJson())

		await().untilAsserted {
			val faktiskNavAnsatt = navAnsattRepository.get(navAnsatt.navIdent)

			assertSoftly(faktiskNavAnsatt.shouldNotBeNull()) {
				navIdent shouldBe navAnsatt.navIdent
				navn shouldBe navAnsatt.navn
				epost shouldBe navAnsatt.epost
				telefon shouldBe navAnsatt.telefon
			}

			val faktiskBruker = navBrukerRepository.get(navBruker.id)

			faktiskBruker.navVeileder.shouldNotBeNull()
			faktiskBruker.navVeileder.navIdent shouldBe navAnsatt.navIdent
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke veileder`() {
		val msg = KafkaMessageCreator.lagTildeltVeilederMsg()
		mockPdlHttpServer.mockHentIdenter(msg.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilTildeltVeilederTopic(msg.toJson())

		withLogCapture(TildeltVeilederConsumer::class.java.name) { loggingEvents ->
			await().untilAsserted {
				navAnsattRepository.get(msg.veilederId) shouldBe null

				loggingEvents.any {
					it.message == "Tildelt veileder endret. Nav-bruker finnes ikke, hopper over Kafka-melding"
				} shouldBe true
			}
		}
	}
}
