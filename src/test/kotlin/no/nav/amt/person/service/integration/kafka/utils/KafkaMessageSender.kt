package no.nav.amt.person.service.integration.kafka.utils

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.amt.person.service.data.kafka.message.EndringPaaBrukerMsg
import no.nav.amt.person.service.data.kafka.message.TildeltVeilederMsg
import no.nav.amt.person.service.integration.IntegrationTestBase.Companion.kafkaContainer
import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.amt.person.service.kafka.consumer.InnsatsgruppeConsumer
import no.nav.amt.person.service.kafka.consumer.dto.SisteOppfolgingsperiodeKafkaPayload
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
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
	@Value($$"${kafka.schema.registry.url}")
	private val schemaRegistryUrl: String,
	@Value($$"${app.env.endringPaaBrukerTopic}")
	private val endringPaaBrukerTopic: String,
	@Value($$"${app.env.sisteTilordnetVeilederTopic}")
	private val sisteTilordnetVeilederTopic: String,
	@Value($$"${app.env.aktorV2Topic}")
	private val aktorV2Topic: String,
	@Value($$"${app.env.skjermedePersonerTopic}")
	private val skjermedePersonerTopic: String,
	@Value($$"${app.env.leesahTopic}")
	private val leesahTopic: String,
	@Value($$"${app.env.oppfolgingsperiodeTopic}")
	private val oppfolgingsperiodeTopic: String,
	@Value($$"${app.env.innsatsgruppeTopic}")
	private val innsatsgruppeTopic: String,
) {
	private val kafkaProducer = KafkaProducerClientImpl<String, String>(properties.producer())

	fun sendTilEndringPaaBrukerTopic(payload: EndringPaaBrukerMsg) = sendTilTopic(endringPaaBrukerTopic, payload)

	fun sendTilTildeltVeilederTopic(payload: TildeltVeilederMsg) = sendTilTopic(sisteTilordnetVeilederTopic, payload)

	fun sendTilOppfolgingsperiodeTopic(payload: SisteOppfolgingsperiodeKafkaPayload) = sendTilTopic(oppfolgingsperiodeTopic, payload)

	fun sendTilInnsatsgruppeTopic(payload: InnsatsgruppeConsumer.Siste14aVedtak) = sendTilTopic(innsatsgruppeTopic, payload)

	fun sendTilSkjermetPersonTopic(
		personident: String,
		erSkjermet: Boolean?,
	) = sendTilTopic(skjermedePersonerTopic, erSkjermet, personident)

	fun sendTilAktorV2Topic(
		key: String,
		value: Aktor,
		schemaId: Int,
	) {
		sendAvroRecord(
			ProducerRecord(aktorV2Topic, key, value).apply {
				headers().add("schemaId", schemaId.toString().toByteArray())
			},
		)
	}

	fun sendTilLeesahTopic(
		key: String,
		value: Personhendelse,
		schemaId: Int,
	) {
		sendAvroRecord(
			ProducerRecord(leesahTopic, key, value).apply {
				headers().add(
					"schemaId",
					schemaId.toString().toByteArray(),
				)
			},
		)
	}

	private fun sendTilTopic(
		topic: String,
		payload: Any?,
		key: String = UUID.randomUUID().toString(),
	) {
		kafkaProducer.send(
			ProducerRecord(topic, key, payload?.let { staticObjectMapper.writeValueAsString(it) }),
		)
	}

	private fun <K, V> sendAvroRecord(record: ProducerRecord<K, V>) {
		KafkaProducer<K, V>(
			Properties().apply {
				put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
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
