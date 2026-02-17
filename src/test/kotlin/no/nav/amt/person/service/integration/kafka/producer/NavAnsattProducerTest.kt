package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer.consume
import no.nav.amt.person.service.integration.mock.responses.MockNavAnsattRespomse
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.navansatt.NavAnsattService
import no.nav.amt.person.service.navansatt.NavAnsattUpdater
import org.junit.jupiter.api.Test

class NavAnsattProducerTest(
	private val kafkaProducerService: KafkaProducerService,
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val navAnsattService: NavAnsattService,
	private val navAnsattUpdater: NavAnsattUpdater,
) : IntegrationTestBase() {
	@Test
	fun `publiserNavAnsatt - skal publisere ansatt med riktig key og value`() {
		val ansatt = TestData.lagNavAnsatt()

		kafkaProducerService.publiserNavAnsatt(ansatt)

		val record =
			consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)
				?.first { it.key() == ansatt.id.toString() }

		val forventetValue = ansattTilV1Json(ansatt)

		record.shouldNotBeNull()
		record.key() shouldBe ansatt.id.toString()
		record.value() shouldBe forventetValue
	}

	@Test
	fun `publiserNavAnsatt - ansatt er oppdatert - skal publisere ny melding`() {
		val ansatt = TestData.lagNavAnsatt()
		testDataRepository.insertNavAnsatt(ansatt)

		val oppdatertAnsatt = ansatt.copy(navn = "nytt navn", telefon = "nytt nummer", epost = "ny@epost.no")
		navAnsattService.upsert(oppdatertAnsatt)

		val record =
			consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)
				?.first { it.key() == ansatt.id.toString() }

		val forventetValue = ansattTilV1Json(oppdatertAnsatt)

		record.shouldNotBeNull()
		record.key() shouldBe ansatt.id.toString()
		record.value() shouldBe forventetValue
	}

	@Test
	fun `publiserNavAnsatt - flere ansatte sjekkes for oppdatering - skal publisere melding kun for de med endring`() {
		val endretAnsatt = TestData.lagNavAnsatt()
		testDataRepository.insertNavAnsatt(endretAnsatt)

		val uendretAnsatt = TestData.lagNavAnsatt()
		testDataRepository.insertNavAnsatt(uendretAnsatt)

		mockNomHttpServer.mockHentNavAnsatt(MockNavAnsattRespomse.fromDbo(endretAnsatt.copy(navn = "nytt navn")))
		mockNomHttpServer.mockHentNavAnsatt(MockNavAnsattRespomse.fromDbo(uendretAnsatt))

		navAnsattUpdater.oppdaterAlle()

		val records = consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)

		records.shouldNotBeNull()
		records.any { it.key() == endretAnsatt.id.toString() } shouldBe true
		records.any { it.key() == uendretAnsatt.id.toString() } shouldBe false
	}

	private fun ansattTilV1Json(ansatt: NavAnsattDbo): String =
		objectMapper.writeValueAsString(
			NavAnsattDtoV1(
				id = ansatt.id,
				navident = ansatt.navIdent,
				navn = ansatt.navn,
				telefon = ansatt.telefon,
				epost = ansatt.epost,
				navEnhetId = ansatt.navEnhetId,
			),
		)
}
