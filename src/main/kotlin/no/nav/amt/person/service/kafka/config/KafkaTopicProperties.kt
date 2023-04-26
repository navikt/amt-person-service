package no.nav.amt.person.service.kafka.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class KafkaTopicProperties(
	val endringPaaBrukerTopic: String,
)
