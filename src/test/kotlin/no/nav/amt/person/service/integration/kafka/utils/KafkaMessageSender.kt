package no.nav.amt.person.service.integration.kafka.utils

import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class KafkaMessageSender(
	properties: KafkaProperties,
	@Value("\${app.env.endringPaaBrukerTopic}")
	private val endringPaaBrukerTopic: String,
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

}
