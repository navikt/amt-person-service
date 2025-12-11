package no.nav.amt.person.service.kafka.config

import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Properties

@Configuration(proxyBeanMethods = false)
class KafkaBeans {
	@Bean
	@Profile("default")
	fun kafkaConsumerProperties(): KafkaProperties =
		object : KafkaProperties {
			override fun consumer(): Properties = KafkaPropertiesPreset.aivenDefaultConsumerProperties("amt-person-service-consumer.v1")

			override fun producer(): Properties = KafkaPropertiesPreset.aivenDefaultProducerProperties("amt-person-service-producer")
		}

	@Bean
	fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClient<String, String> =
		KafkaProducerClientImpl(kafkaProperties.producer())
}
