package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.amt.person.service.utils.LogUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TildeltVeilederConsumerTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Autowired
	lateinit var navAnsattService: NavAnsattService


	@Test
	fun `ingest - bruker finnes, ny veileder - oppretter og oppdaterer nav veileder`() {
		val navBruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(navBruker)

		val msg = KafkaMessageCreator.lagTildeltVeilederMsg()

		val navAnsatt = TestData.lagNavAnsatt(navIdent = msg.veilederId)


		mockPdlHttpServer.mockHentIdenter(msg.aktorId, navBruker.person.personident)
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())
		mockNorgHttpServer.addNavAnsattEnhet()

		kafkaMessageSender.sendTilTildeltVeilederTopic(msg.toJson())


		AsyncUtils.eventually {
			val faktiskNavAnsatt = navAnsattService.hentNavAnsatt(navAnsatt.navIdent)

			faktiskNavAnsatt!!.navIdent shouldBe navAnsatt.navIdent
			faktiskNavAnsatt.navn shouldBe navAnsatt.navn
			faktiskNavAnsatt.epost shouldBe navAnsatt.epost
			faktiskNavAnsatt.telefon shouldBe navAnsatt.telefon

			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.navVeileder!!.navIdent shouldBe navAnsatt.navIdent

		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke veileder`() {
		val msg = KafkaMessageCreator.lagTildeltVeilederMsg()
		mockPdlHttpServer.mockHentIdenter(msg.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilTildeltVeilederTopic(msg.toJson())

		LogUtils.withLogs { getLogs ->
			AsyncUtils.eventually {
				getLogs().any {
					it.message == "Tildelt veileder endret. NavBruker finnes ikke, hopper over kafka melding"
				} shouldBe true

				navAnsattService.hentNavAnsatt(msg.veilederId) shouldBe null
			}
		}
	}

}
