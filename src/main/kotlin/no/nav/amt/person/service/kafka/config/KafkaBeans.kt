package no.nav.amt.person.service.kafka.config

import no.nav.amt.person.service.kafka.config.KafkaTopicProperties.Companion.CONSUMER_GROUP_ID
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties.Companion.PRODUCER_ID
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
			override fun consumer(): Properties = KafkaPropertiesPreset.aivenDefaultConsumerProperties(CONSUMER_GROUP_ID)

			override fun producer(): Properties = KafkaPropertiesPreset.aivenDefaultProducerProperties(PRODUCER_ID)
		}

	@Bean
	fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClient<String, String> =
		KafkaProducerClientImpl(kafkaProperties.producer())
}
