package no.nav.amt.person.service.integration.kafka.utils

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

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
) {
	private val kafkaProducer = KafkaProducerClientImpl<ByteArray, ByteArray>(properties.producer())

	fun sendTilEndringPaaBrukerTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				endringPaaBrukerTopic,
				UUID.randomUUID().toString().toByteArray(),
				jsonString.toByteArray(),
			)
		)
	}

	fun sendTilTildeltVeilederTopic(jsonString: String) {
		kafkaProducer.send(
			ProducerRecord(
				sisteTilordnetVeilederTopic,
				UUID.randomUUID().toString().toByteArray(),
				jsonString.toByteArray(),
			)
		)
	}

	fun sendTilAktorV2Topic(key: String, value: Aktor, schemaId: Int) {
		val record = ProducerRecord(aktorV2Topic, key, value)
		record.headers().add("schemaId", schemaId.toString().toByteArray())

		KafkaProducer<String, Aktor>(Properties().apply {
			put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SingletonKafkaProvider.getHost())
			put(KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS, true)
			put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
			put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
			put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
		}).use { producer ->
			producer.send(record).get()
		}

	}

}