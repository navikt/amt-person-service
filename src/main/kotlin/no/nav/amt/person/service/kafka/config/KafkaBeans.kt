package no.nav.amt.person.service.kafka.config

import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.apache.kafka.common.serialization.ByteArrayDeserializer
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
				return KafkaPropertiesBuilder.consumerBuilder()
						.withBaseProperties()
						.withConsumerGroupId("amt-person-service-consumer.v1")
						.withAivenBrokerUrl()
						.withAivenAuth()
						.withDeserializers(
							ByteArrayDeserializer::class.java,
							ByteArrayDeserializer::class.java
						)
						.build()
			}

			override fun producer(): Properties {
				return KafkaPropertiesPreset.aivenByteProducerProperties("amt-person-service-producer")
			}
		}
	}
}
