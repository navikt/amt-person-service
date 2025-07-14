package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer.consume
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_ansatt.NavAnsattUpdater
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.Test

class NavAnsattProducerTest(
	private val kafkaProducerService: KafkaProducerService,
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val navAnsattService: NavAnsattService,
	private val navAnsattUpdater: NavAnsattUpdater
) : IntegrationTestBase() {

	@Test
	fun `publiserNavAnsatt - skal publisere ansatt med riktig key og value`() {
		val ansatt = TestData.lagNavAnsatt().toModel()

		kafkaProducerService.publiserNavAnsatt(ansatt)

		val record = consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)
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

		val oppdatertAnsatt = ansatt.copy(navn = "nytt navn", telefon = "nytt nummer", epost = "ny@epost.no").toModel()
		navAnsattService.upsert(oppdatertAnsatt)

		val record = consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)
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

		mockNomHttpServer.mockHentNavAnsatt(endretAnsatt.toModel().copy(navn = "nytt navn"))
		mockNomHttpServer.mockHentNavAnsatt(uendretAnsatt.toModel())

		navAnsattUpdater.oppdaterAlle()

		val records = consume(kafkaTopicProperties.amtNavAnsattPersonaliaTopic)

		records.shouldNotBeNull()
		records.any { it.key() == endretAnsatt.id.toString() } shouldBe true
		records.any { it.key() == uendretAnsatt.id.toString() } shouldBe false
	}

	private fun ansattTilV1Json(ansatt: NavAnsatt): String {
		return toJsonString(
			NavAnsattDtoV1(
				id = ansatt.id,
				navident = ansatt.navIdent,
				navn = ansatt.navn,
				telefon = ansatt.telefon,
				epost = ansatt.epost,
				navEnhetId = ansatt.navEnhetId,
			)
		)
	}
}
