package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.api.dto.toDto
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer.consume
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.utils.JsonUtils
import org.junit.jupiter.api.Test

class NavEnhetProducerTest(
	private val kafkaProducerService: KafkaProducerService,
	private val kafkaTopicProperties: KafkaTopicProperties,
) : IntegrationTestBase() {
	@Test
	fun `publiserNavEnhet - skal publisere enhet med riktig key og value`() {
		val navEnhet = TestData.lagNavEnhet().toModel()

		kafkaProducerService.publiserNavEnhet(navEnhet)

		val records = consume(kafkaTopicProperties.amtNavEnhetTopic)
		records.shouldNotBeNull()
		val record = records.first { it.key() == navEnhet.id.toString() }

		val forventetValue = JsonUtils.toJsonString(navEnhet.toDto())

		record.key() shouldBe navEnhet.id.toString()
		record.value() shouldBe forventetValue
	}
}
