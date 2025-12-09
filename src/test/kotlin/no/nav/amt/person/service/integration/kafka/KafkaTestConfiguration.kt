package no.nav.amt.person.service.integration.kafka

import no.nav.amt.person.service.integration.kafka.utils.SingletonKafkaProvider
import no.nav.amt.person.service.kafka.config.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class KafkaTestConfiguration {
	@Bean
	fun kafkaProperties(): KafkaProperties = SingletonKafkaProvider.getKafkaProperties()
}
