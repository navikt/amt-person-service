package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import no.nav.amt.person.service.utils.LogUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class InnsatsgruppeV2ConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerService: NavBrukerService,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes, ny innsatsgruppe - oppdaterer`() {
		testDataRepository.insertNavBruker(navBruker)

		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, navBruker.person.personident)
		kafkaMessageSender.sendTilGjeldende14aVedtakTopic(objectMapper.writeValueAsString(siste14aVedtak))

		await().untilAsserted {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.innsatsgruppe shouldBe InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		mockPdlHttpServer.mockHentIdenter(siste14aVedtak.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilGjeldende14aVedtakTopic(objectMapper.writeValueAsString(siste14aVedtak))

		LogUtils.withLogs { getLogs ->
			await().untilAsserted {
				getLogs().any {
					it.message == "Innsatsgruppe endret. NavBruker finnes ikke, hopper over kafkamelding"
				} shouldBe true
			}
		}
	}

	companion object {
		private val navBruker = TestData.lagNavBruker(innsatsgruppe = InnsatsgruppeV1.STANDARD_INNSATS)

		private val siste14aVedtak =
			InnsatsgruppeV2Consumer.Gjeldende14aVedtakKafkaPayload(
				aktorId = navBruker.person.personident,
				innsatsgruppe = InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
				hovedmal = null,
				fattetDato = ZonedDateTime.now(),
				vedtakId = "1234",
			)
	}
}
