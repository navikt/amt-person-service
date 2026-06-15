package no.nav.amt.person.service.kafka.config

import no.nav.amt.person.service.kafka.config.KafkaTopicProperties.Companion.CONSUMER_GROUP_ID
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties.Companion.PRODUCER_ID
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Properties

@Configuration(proxyBeanMethods = false)
class KafkaBeans {
    @Bean
    @Profile("default")
    fun kafkaConsumerProperties(): KafkaProperties = object : KafkaProperties {
        override fun consumer(): Properties = KafkaPropertiesPreset.aivenDefaultConsumerProperties(CONSUMER_GROUP_ID)

        override fun producer(): Properties = KafkaPropertiesPreset.aivenDefaultProducerProperties(PRODUCER_ID)
    }

    @Bean
    @Profile("local")
    fun kafkaLocalProperties(): KafkaProperties = object : KafkaProperties {
        override fun consumer(): Properties = KafkaPropertiesBuilder
            .consumerBuilder()
            .withBrokerUrl("localhost:9092")
            .withBaseProperties()
            .withConsumerGroupId("amt-person-service-local-consumer")
            .withDeserializers(StringDeserializer::class.java, StringDeserializer::class.java)
            .build()

        override fun producer(): Properties = KafkaPropertiesBuilder
            .producerBuilder()
            .withBrokerUrl("localhost:9092")
            .withBaseProperties()
            .withProducerId("amt-person-service-local-producer")
            .withSerializers(StringSerializer::class.java, StringSerializer::class.java)
            .build()
    }

    @Bean
    fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClient<String, String> =
        KafkaProducerClientImpl(kafkaProperties.producer())
}
