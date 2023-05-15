package no.nav.amt.person.service.kafka.config

import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.*

@Configuration
class KafkaBeans {
	@Bean
	@Profile("default")
	fun kafkaConsumerProperties(): KafkaProperties {
		return object : KafkaProperties {
			override fun consumer(): Properties {
				return KafkaPropertiesPreset.aivenDefaultConsumerProperties("amt-person-service-consumer.v1")
			}

			override fun producer(): Properties {
				return KafkaPropertiesPreset.aivenDefaultProducerProperties("amt-person-service-producer")
			}
		}
	}


	@Bean
	fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClient<String, String> {
		return KafkaProducerClientImpl(kafkaProperties.producer())
	}
}
