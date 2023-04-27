package no.nav.amt.person.service.kafka.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.amt.person.service.kafka.ingestor.EndringPaaBrukerIngestor
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import java.util.function.Consumer

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConfiguration(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	jdbcTemplate: JdbcTemplate,
	endringPaaBrukerIngestor: EndringPaaBrukerIngestor,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private var consumerRepository = PostgresJdbcTemplateConsumerRepository(jdbcTemplate)

	private lateinit var client: KafkaConsumerClient
	private lateinit var consumerRecordProcessor: KafkaConsumerRecordProcessor

	init {
		val topicConfigs = listOf(
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.endringPaaBrukerTopic,
					Deserializers.stringDeserializer(),
					Deserializers.stringDeserializer(),
					Consumer<ConsumerRecord<String, String>> { endringPaaBrukerIngestor.ingest(it.value()) }
				),
		)

		consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
			.builder()
			.withLockProvider(JdbcTemplateLockProvider(jdbcTemplate))
			.withKafkaConsumerRepository(consumerRepository)
			.withConsumerConfigs(topicConfigs.map { it.consumerConfig })
			.build()

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(topicConfigs)
			.build()
	}
	@EventListener
	fun onApplicationEvent(_event: ContextRefreshedEvent?) {
		log.info("Starting kafka consumer and stored record processor...")
		client.start()
		consumerRecordProcessor.start()
	}

}
