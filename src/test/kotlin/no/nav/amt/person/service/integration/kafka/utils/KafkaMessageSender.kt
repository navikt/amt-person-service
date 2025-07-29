package no.nav.amt.person.service.integration.kafka.utils

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Properties
import java.util.UUID

@Component
class KafkaMessageSender(
	properties: KafkaProperties,
	@Value("\${kafka.schema.registry.url}")
	private val schemaRegistryUrl: String,
	@Value("\${app.env.endringPaaBrukerTopic}")
	private val endringPaaBrukerTopic: String,
	@Value("\${app.env.sisteTilordnetVeilederTopic}")
	private val sisteTilordnetVeilederTopic: String,
	@Value("\${app.env.aktorV2Topic}")
	private val aktorV2Topic: String,
	@Value("\${app.env.skjermedePersonerTopic}")
	private val skjermedePersonerTopic: String,
	@Value("\${app.env.leesahTopic}")
	private val leesahTopic: String,
	@Value("\${app.env.oppfolgingsperiodeTopic}")
	private val oppfolgingsperiodeTopic: String,
	@Value("\${app.env.innsatsgruppeTopic}")
	private val innsatsgruppeTopic: String,
) {
	private val kafkaProducer = KafkaProducerClientImpl<String, String>(properties.producer())

	fun sendTilEndringPaaBrukerTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				endringPaaBrukerTopic,
				UUID.randomUUID().toString(),
				jsonString,
			),
		)
	}

	fun sendTilTildeltVeilederTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				sisteTilordnetVeilederTopic,
				UUID.randomUUID().toString(),
				jsonString,
			),
		)
	}

	fun sendTilOppfolgingsperiodeTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				oppfolgingsperiodeTopic,
				UUID.randomUUID().toString(),
				jsonString,
			),
		)
	}

	fun sendTilInnsatsgruppeTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				innsatsgruppeTopic,
				UUID.randomUUID().toString(),
				jsonString,
			),
		)
	}

	fun sendTilSkjermetPersonTopic(
		personident: String,
		erSkjermet: Boolean,
	) {
		kafkaProducer.send(
			ProducerRecord(
				skjermedePersonerTopic,
				personident,
				erSkjermet.toString(),
			),
		)
	}

	fun sendTilAktorV2Topic(
		key: String,
		value: Aktor,
		schemaId: Int,
	) {
		val record = ProducerRecord(aktorV2Topic, key, value)
		record.headers().add("schemaId", schemaId.toString().toByteArray())

		sendAvroRecord(record)
	}

	fun sendTilLeesahTopic(
		key: String,
		value: Personhendelse,
		schemaId: Int,
	) {
		val record = ProducerRecord(leesahTopic, key, value)
		record.headers().add("schemaId", schemaId.toString().toByteArray())

		sendAvroRecord(record)
	}

	private fun <K, V> sendAvroRecord(record: ProducerRecord<K, V>) {
		KafkaProducer<K, V>(
			Properties().apply {
				put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SingletonKafkaProvider.getHost())
				put(KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS, true)
				put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
				put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
				put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
			},
		).use { producer ->
			producer.send(record).get()
		}
	}
}
