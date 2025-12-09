package no.nav.amt.person.service.integration.kafka

import no.nav.amt.person.service.integration.IntegrationTestBase.Companion.kafkaContainer
import no.nav.amt.person.service.kafka.config.KafkaProperties
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.Properties

@TestConfiguration
class KafkaTestConfiguration {
	@Bean
	fun kafkaProperties() =
		object : KafkaProperties {
			override fun consumer(): Properties =
				KafkaPropertiesBuilder
					.consumerBuilder()
					.withBrokerUrl(kafkaContainer.bootstrapServers)
					.withBaseProperties()
					.withConsumerGroupId(CONSUMER_ID)
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()

			override fun producer(): Properties =
				KafkaPropertiesBuilder
					.producerBuilder()
					.withBrokerUrl(kafkaContainer.bootstrapServers)
					.withBaseProperties()
					.withProducerId(PRODUCER_ID)
					.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
					.build()
		}

	companion object {
		private const val PRODUCER_ID = "INTEGRATION_PRODUCER"
		private const val CONSUMER_ID = "INTEGRATION_CONSUMER"
	}
}
