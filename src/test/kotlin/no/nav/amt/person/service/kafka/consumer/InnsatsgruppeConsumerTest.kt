package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.Innsatsgruppe
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.amt.person.service.utils.LogUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InnsatsgruppeConsumerTest : IntegrationTestBase() {
	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Test
	fun `ingest - bruker finnes, ny innsatsgruppe - oppdaterer`() {
		val navBruker = TestData.lagNavBruker(innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS)
		testDataRepository.insertNavBruker(navBruker)

		val siste14aVedtak = InnsatsgruppeConsumer.Siste14aVedtak(
			aktorId = navBruker.person.personident,
			innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
		)
		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, navBruker.person.personident)

		kafkaMessageSender.sendTilInnsatsgruppeTopic(JsonUtils.toJsonString(siste14aVedtak))


		AsyncUtils.eventually {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.innsatsgruppe shouldBe Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		val siste14aVedtak = InnsatsgruppeConsumer.Siste14aVedtak(
			aktorId = "1234",
			innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
		)
		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilInnsatsgruppeTopic(JsonUtils.toJsonString(siste14aVedtak))

		LogUtils.withLogs { getLogs ->
			AsyncUtils.eventually {
				getLogs().any {
					it.message == "Innsatsgruppe endret. NavBruker finnes ikke, hopper over kafkamelding"
				} shouldBe true
			}
		}
	}
}
