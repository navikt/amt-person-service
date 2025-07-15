package no.nav.amt.person.service.kafka.consumer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.LogUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsperiodeConsumerTest : IntegrationTestBase() {
	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Test
	fun `ingest - bruker finnes, ny oppfolgingsperiode - oppdaterer`() {
		val navBruker = TestData.lagNavBruker(oppfolgingsperioder = emptyList())
		testDataRepository.insertNavBruker(navBruker)

		val sisteOppfolgingsperiodeV1 = OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
			uuid = UUID.randomUUID(),
			aktorId = navBruker.person.personident,
			startDato = ZonedDateTime.now().minusWeeks(1),
			sluttDato = null
		)
		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, navBruker.person.personident)

		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(toJsonString(sisteOppfolgingsperiodeV1))


		AsyncUtils.eventually {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.id)

			faktiskBruker.oppfolgingsperioder.size shouldBe 1
			faktiskBruker.oppfolgingsperioder.first().id shouldBe sisteOppfolgingsperiodeV1.uuid
		}
	}

	@Test
	fun `ingest - bruker finnes ikke - oppdaterer ikke`() {
		val sisteOppfolgingsperiodeV1 = OppfolgingsperiodeConsumer.SisteOppfolgingsperiodeV1(
			uuid = UUID.randomUUID(),
			aktorId = "1234",
			startDato = ZonedDateTime.now().minusWeeks(1),
			sluttDato = null
		)
		mockPdlHttpServer.mockHentIdenter(sisteOppfolgingsperiodeV1.aktorId, "ukjent ident")
		kafkaMessageSender.sendTilOppfolgingsperiodeTopic(toJsonString(sisteOppfolgingsperiodeV1))

		LogUtils.withLogs { getLogs ->
			AsyncUtils.eventually {
				getLogs().any {
					it.message == "Oppfølgingsperiode endret. NavBruker finnes ikke, hopper over kafka melding"
				} shouldBe true
			}
		}
	}
}
