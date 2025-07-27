package no.nav.amt.person.service.kafka.consumer

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData.lagNavBruker
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.LogUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsperiodeConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerService: NavBrukerService
) : IntegrationTestBase() {

	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `ingest - bruker finnes, ny oppfolgingsperiode - oppdaterer`(useEndDate: Boolean) {
		val navBruker = lagNavBruker(oppfolgingsperioder = emptyList())
		testDataRepository.insertNavBruker(navBruker)

		val sisteOppfolgingsperiodeV1 = createSisteOppfolgingsperiodeV1(
			personIdent = navBruker.person.personident,
			useEndDate = useEndDate
		)

		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, navBruker.person.personident)

		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(toJsonString(sisteOppfolgingsperiodeV1))

		await().untilAsserted {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)
			faktiskBruker.oppfolgingsperioder.size shouldBe 1

			assertSoftly(faktiskBruker.oppfolgingsperioder.first()) {
				id shouldBe sisteOppfolgingsperiodeV1.uuid
				startdato shouldBe nowAsLocalDateTime

				if (useEndDate) {
					sluttdato shouldBe nowAsLocalDateTime.plusDays(1)
				} else {
					sluttdato shouldBe null
				}
			}
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		val sisteOppfolgingsperiodeV1 = OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
			uuid = UUID.randomUUID(),
			aktorId = AKTOR_ID_IN_TEST,
			startDato = ZonedDateTime.now().minusWeeks(1),
			sluttDato = null
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

	companion object {
		private const val AKTOR_ID_IN_TEST = "1234"
		private val nowAsLocalDateTime = LocalDateTime.now()
		private val nowAsZonedDateTime = ZonedDateTime.of(nowAsLocalDateTime, ZoneId.systemDefault())

		private fun createSisteOppfolgingsperiodeV1(
			personIdent: String,
			useEndDate: Boolean
		) =
			OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
				uuid = UUID.randomUUID(),
				aktorId = personIdent,
				startDato = nowAsZonedDateTime,
				sluttDato = if (useEndDate) nowAsZonedDateTime.plusDays(1) else null,
			)
	}
}
