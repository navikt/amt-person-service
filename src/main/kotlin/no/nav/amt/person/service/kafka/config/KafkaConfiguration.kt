package no.nav.amt.person.service.kafka.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("kafka.enabled", havingValue = "true", matchIfMissing = true)
class KafkaConfiguration(
    private val kafkaProperties: KafkaProperties,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Bean
    fun kafkaConsumerClient(topicConfigs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>): KafkaConsumerClient =
        KafkaConsumerClientBuilder
            .builder()
            .withProperties(kafkaProperties.consumer())
            .withTopicConfigs(topicConfigs)
            .build()

    @Bean
    fun kafkaConsumerRecordProcessor(
        topicConfigs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>,
        consumerRepository: no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository,
    ): KafkaConsumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
        .builder()
        .withLockProvider(JdbcTemplateLockProvider(jdbcTemplate))
        .withKafkaConsumerRepository(consumerRepository)
        .withConsumerConfigs(topicConfigs.map { it.consumerConfig })
        .build()
}
