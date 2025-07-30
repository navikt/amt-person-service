package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.LogUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsperiodeConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerService: NavBrukerService,
) : IntegrationTestBase() {
	@Test
	fun `ingest - bruker finnes, ny oppfolgingsperiode - oppdaterer`() {
		val navBruker = TestData.lagNavBruker(oppfolgingsperioder = emptyList())
		testDataRepository.insertNavBruker(navBruker)

		val sisteOppfolgingsperiodeV1 =
			OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
				uuid = UUID.randomUUID(),
				aktorId = navBruker.person.personident,
				startDato = ZonedDateTime.now().minusWeeks(1),
				sluttDato = null,
			)
		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, navBruker.person.personident)

		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(toJsonString(sisteOppfolgingsperiodeV1))

		await().untilAsserted {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.oppfolgingsperioder.size shouldBe 1
			faktiskBruker.oppfolgingsperioder.first().id shouldBe sisteOppfolgingsperiodeV1.uuid
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		val sisteOppfolgingsperiodeV1 =
			OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
				uuid = UUID.randomUUID(),
				aktorId = "1234",
				startDato = ZonedDateTime.now().minusWeeks(1),
				sluttDato = null,
			)
		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(toJsonString(sisteOppfolgingsperiodeV1))

		LogUtils.withLogs { getLogs ->
			await().untilAsserted {
				getLogs().any {
					it.message == "Oppfølgingsperiode endret. NavBruker finnes ikke, hopper over kafka melding"
				} shouldBe true
			}
		}
	}
}
