package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.SingletonKafkaProvider
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.utils.JsonUtils
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.*

class KafkaProducerServiceTest: IntegrationTestBase() {

	@Autowired
	lateinit var kafkaProducerService: KafkaProducerService

	@Autowired
	lateinit var kafkaTopicProperties: KafkaTopicProperties

	@Test
	fun `publiserNavBruker - skal publisere bruker med riktig key og value`() {
		val navBruker = TestData.lagNavBruker().toModel()

		kafkaProducerService.publiserNavBruker(navBruker)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == navBruker.id.toString() }

		val forventetValue = JsonUtils.toJsonString(
			NavBrukerDtoV1(
				id = navBruker.id,
				personIdent = navBruker.person.personIdent,
				personIdentType = navBruker.person.personIdentType,
				fornavn = navBruker.person.fornavn,
				mellomnavn = navBruker.person.mellomnavn,
				etternavn = navBruker.person.etternavn,
				navVeilederId = navBruker.navVeileder?.id,
				navEnhet = navBruker.navEnhet?.let { NavEnhetDtoV1(it.id, it.enhetId, it.navn) },
				telefon = navBruker.telefon,
				epost = navBruker.epost,
				erSkjermet = navBruker.erSkjermet,
			)
		)

		record.key() shouldBe navBruker.id.toString()
		record.value() shouldBe forventetValue
	}

	@Test
	fun `publiserSlettNavBruker - skal publisere tombstone med riktig key og null value`() {
		val brukerId = UUID.randomUUID()

		kafkaProducerService.publiserSlettNavBruker(brukerId)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == brukerId.toString() }

		record.value() shouldBe null
	}

	private fun consume(topic: String): ConsumerRecords<String, String>? {
		val consumer = KafkaConsumer<String, String>(Properties().apply {
			put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SingletonKafkaProvider.getHost())
			put(ConsumerConfig.GROUP_ID_CONFIG, "CONSUMER_ID")
			put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
			put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
			put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
		})

		consumer.subscribe(listOf(topic))
		val records =  consumer.poll(Duration.ofSeconds(5))
		consumer.close()

		return records
	}

}
