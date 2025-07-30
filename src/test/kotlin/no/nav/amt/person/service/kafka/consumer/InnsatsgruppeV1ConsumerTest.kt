package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.amt.person.service.utils.LogUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class InnsatsgruppeV1ConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerService: NavBrukerService,
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
		kafkaMessageSender.sendTilInnsatsgruppeTopic(JsonUtils.toJsonString(siste14aVedtak))

		await().untilAsserted {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

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
		kafkaMessageSender.sendTilInnsatsgruppeTopic(JsonUtils.toJsonString(siste14aVedtak))

		LogUtils.withLogs { getLogs ->
			await().untilAsserted {
				getLogs().any {
					it.message == "Innsatsgruppe endret. NavBruker finnes ikke, hopper over kafkamelding"
				} shouldBe true
			}
		}
	}
}
