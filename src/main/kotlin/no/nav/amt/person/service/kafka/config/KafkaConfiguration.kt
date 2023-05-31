package no.nav.amt.person.service.kafka.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.amt.person.service.kafka.ingestor.*
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import java.util.function.Consumer

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConfiguration(
	@Value("\${kafka.schema.registry.url}") schemaRegistryUrl: String,
	@Value("\${kafka.schema.registry.username}") schemaRegistryUsername: String,
	@Value("\${kafka.schema.registry.password}") schemaRegistryPassword: String,
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	jdbcTemplate: JdbcTemplate,
	endringPaaBrukerIngestor: EndringPaaBrukerIngestor,
	tildeltVeilederIngestor: TildeltVeilederIngestor,
	aktorV2Ingestor: AktorV2Ingestor,
	skjermetPersonIngestor: SkjermetPersonIngestor,
	leesahIngestor: LeesahIngestor,
	deltakerIngestor: DeltakerV2Ingestor,
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
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.sisteTilordnetVeilederTopic,
					Deserializers.stringDeserializer(),
					Deserializers.stringDeserializer(),
					Consumer<ConsumerRecord<String, String>> { tildeltVeilederIngestor.ingest(it.value()) }
				),
			KafkaConsumerClientBuilder.TopicConfig<String, Aktor>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.aktorV2Topic,
					Deserializers.stringDeserializer(),
					SpecificAvroDeserializer(schemaRegistryUrl, schemaRegistryUsername, schemaRegistryPassword),
					Consumer<ConsumerRecord<String, Aktor>> { aktorV2Ingestor.ingest(it.key(), it.value()) }
				),
			KafkaConsumerClientBuilder.TopicConfig<String, Personhendelse>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.leesahTopic,
					Deserializers.stringDeserializer(),
					SpecificAvroDeserializer(schemaRegistryUrl, schemaRegistryUsername, schemaRegistryPassword),
					Consumer<ConsumerRecord<String, Personhendelse>> { leesahIngestor.ingest(it.value()) }
				),
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.skjermedePersonerTopic,
					Deserializers.stringDeserializer(),
					Deserializers.stringDeserializer(),
					Consumer<ConsumerRecord<String, String>> { skjermetPersonIngestor.ingest(it.key(), it.value()) }
				),
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withStoreOnFailure(consumerRepository)
				.withConsumerConfig(
					kafkaTopicProperties.deltakerV2Topic,
					Deserializers.stringDeserializer(),
					Deserializers.stringDeserializer(),
					Consumer<ConsumerRecord<String, String>> { deltakerIngestor.ingest(it.key(), it.value()) }
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
