package no.nav.amt.person.service.kafka.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.amt.person.service.kafka.consumer.AktorV2Consumer
import no.nav.amt.person.service.kafka.consumer.EndringPaaBrukerConsumer
import no.nav.amt.person.service.kafka.consumer.InnsatsgruppeConsumer
import no.nav.amt.person.service.kafka.consumer.LeesahConsumer
import no.nav.amt.person.service.kafka.consumer.OppfolgingsperiodeConsumer
import no.nav.amt.person.service.kafka.consumer.SkjermetPersonConsumer
import no.nav.amt.person.service.kafka.consumer.TildeltVeilederConsumer
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import java.util.function.Consumer

@EnableConfigurationProperties(KafkaTopicProperties::class)
@Configuration(proxyBeanMethods = false)
class KafkaConfiguration(
	@Value($$"${kafka.schema.registry.url}") schemaRegistryUrl: String,
	@Value($$"${kafka.schema.registry.username}") schemaRegistryUsername: String,
	@Value($$"${kafka.schema.registry.password}") schemaRegistryPassword: String,
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	jdbcTemplate: JdbcTemplate,
	endringPaaBrukerConsumer: EndringPaaBrukerConsumer,
	tildeltVeilederConsumer: TildeltVeilederConsumer,
	aktorV2Consumer: AktorV2Consumer,
	skjermetPersonConsumer: SkjermetPersonConsumer,
	leesahConsumer: LeesahConsumer,
	oppfolgingsperiodeConsumer: OppfolgingsperiodeConsumer,
	innsatsgruppeConsumer: InnsatsgruppeConsumer,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val consumerRepository = PostgresJdbcTemplateConsumerRepository(jdbcTemplate)

	private val client: KafkaConsumerClient
	private val consumerRecordProcessor: KafkaConsumerRecordProcessor

	init {
		val topicConfigs =
			listOf(
				KafkaConsumerClientBuilder
					.TopicConfig<String, String>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.endringPaaBrukerTopic,
						Deserializers.stringDeserializer(),
						Deserializers.stringDeserializer(),
						Consumer { endringPaaBrukerConsumer.ingest(it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, String>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.sisteTilordnetVeilederTopic,
						Deserializers.stringDeserializer(),
						Deserializers.stringDeserializer(),
						Consumer { tildeltVeilederConsumer.ingest(it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, String>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.oppfolgingsperiodeTopic,
						Deserializers.stringDeserializer(),
						Deserializers.stringDeserializer(),
						Consumer { oppfolgingsperiodeConsumer.ingest(it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, String>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.innsatsgruppeTopic,
						Deserializers.stringDeserializer(),
						Deserializers.stringDeserializer(),
						Consumer { innsatsgruppeConsumer.ingest(it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, Aktor>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.aktorV2Topic,
						Deserializers.stringDeserializer(),
						SpecificAvroDeserializer(schemaRegistryUrl, schemaRegistryUsername, schemaRegistryPassword),
						Consumer { aktorV2Consumer.ingest(it.key(), it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, Personhendelse>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.leesahTopic,
						Deserializers.stringDeserializer(),
						SpecificAvroDeserializer(schemaRegistryUrl, schemaRegistryUsername, schemaRegistryPassword),
						Consumer { leesahConsumer.ingest(it.value()) },
					),
				KafkaConsumerClientBuilder
					.TopicConfig<String, String>()
					.withLogging()
					.withStoreOnFailure(consumerRepository)
					.withConsumerConfig(
						kafkaTopicProperties.skjermedePersonerTopic,
						Deserializers.stringDeserializer(),
						Deserializers.stringDeserializer(),
						Consumer { skjermetPersonConsumer.ingest(it.key(), it.value()) },
					),
			)

		consumerRecordProcessor =
			KafkaConsumerRecordProcessorBuilder
				.builder()
				.withLockProvider(JdbcTemplateLockProvider(jdbcTemplate))
				.withKafkaConsumerRepository(consumerRepository)
				.withConsumerConfigs(topicConfigs.map { it.consumerConfig })
				.build()

		client =
			KafkaConsumerClientBuilder
				.builder()
				.withProperties(kafkaProperties.consumer())
				.withTopicConfigs(topicConfigs)
				.build()
	}

	@EventListener
	@Suppress("unused")
	fun onApplicationEvent(event: ContextRefreshedEvent?) {
		log.info("Starting Kafka consumer and stored record processor...")
		client.start()
		consumerRecordProcessor.start()
	}
}
