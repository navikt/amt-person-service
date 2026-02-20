package no.nav.amt.person.service.kafka.consumer

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.person.service.data.TestData.lagNavBruker
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.kafka.consumer.dto.SisteOppfolgingsperiodeKafkaPayload
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsperiodeConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val navBrukerRepository: NavBrukerRepository,
) : IntegrationTestBase() {
	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `ingest - bruker finnes, ny oppfolgingsperiode - oppdaterer`(useEndDate: Boolean) {
		val navBruker = lagNavBruker(oppfolgingsperioder = emptyList())
		testDataRepository.insertNavBruker(navBruker)

		val sisteOppfolgingsperiodeV1 =
			createSisteOppfolgingsperiodeV1(
				personIdent = navBruker.person.personident,
				useEndDate = useEndDate,
			)

		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, navBruker.person.personident)
		mockVeilarbvedtaksstotteHttpServer.mockHentInnsatsgruppe(
			navBruker.person.personident,
			InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
		)

		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(sisteOppfolgingsperiodeV1)

		await().untilAsserted {
			val faktiskBruker = navBrukerRepository.get(navBruker.id)
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
		val sisteOppfolgingsperiodeV1 =
			SisteOppfolgingsperiodeKafkaPayload(
				uuid = UUID.randomUUID(),
				aktorId = AKTOR_ID_IN_TEST,
				startDato = ZonedDateTime.now().minusWeeks(1),
				sluttDato = null,
			)

		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(sisteOppfolgingsperiodeV1)

		withLogCapture(OppfolgingsperiodeConsumer::class.java.name) { loggingEvents ->
			await().untilAsserted {
				loggingEvents.map { it.message } shouldContain
					"Nav-bruker finnes ikke i tabellen nav_bruker, dropper videre prosessering"
			}
		}
	}

	companion object {
		private const val AKTOR_ID_IN_TEST = "1234"

		private val nowAsZonedDateTimeUtc: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
		private val nowAsLocalDateTime: LocalDateTime =
			nowAsZonedDateTimeUtc
				.withZoneSameInstant(ZoneId.systemDefault())
				.toLocalDateTime()

		private fun createSisteOppfolgingsperiodeV1(
			personIdent: String,
			useEndDate: Boolean,
		) = SisteOppfolgingsperiodeKafkaPayload(
			uuid = UUID.randomUUID(),
			aktorId = personIdent,
			startDato = nowAsZonedDateTimeUtc,
			sluttDato = if (useEndDate) nowAsZonedDateTimeUtc.plusDays(1) else null,
		)
	}
}
