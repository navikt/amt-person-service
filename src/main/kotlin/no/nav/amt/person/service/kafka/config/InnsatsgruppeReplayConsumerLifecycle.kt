package no.nav.amt.person.service.kafka.config

import no.nav.amt.person.service.kafka.consumer.InnsatsgruppeConsumer
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * MIDLERTIDIG komponent for å lese innsatsgruppe-topicet på nytt fra start (earliest),
 * uten å påvirke consumer-groupen som brukes av det ordinære oppsettet i [KafkaTopicConfiguration].
 *
 * Kjører som en egen, uavhengig [KafkaConsumerClient] med sin egen consumer-group-id, parallelt
 * med den ordinære consumeren. [InnsatsgruppeConsumer.ingest] er idempotent (upsert), så det er
 * trygt at begge groupene prosesserer topicet samtidig.
 *
 * Aktiveres kun når `app.replay.innsatsgruppe.enabled=true` (f.eks. som miljøvariabel i nais.yaml).
 *
 * FJERN denne klassen og property-en igjen når re-lesingen er ferdig.
 */
@Component
@ConditionalOnProperty("kafka.enabled", havingValue = "true", matchIfMissing = true)
class InnsatsgruppeReplayConsumerLifecycle(
    kafkaTopicProperties: KafkaTopicProperties,
    innsatsgruppeConsumer: InnsatsgruppeConsumer,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(javaClass)

    private val replayConsumerGroupId = "${KafkaTopicProperties.CONSUMER_GROUP_ID}-innsatsgruppe-replay"

    private val client: KafkaConsumerClient = KafkaConsumerClientBuilder
        .builder()
        .withProperties(KafkaPropertiesPreset.aivenDefaultConsumerProperties(replayConsumerGroupId))
        .withTopicConfigs(
            listOf(
                KafkaConsumerClientBuilder
                    .TopicConfig<String, String>()
                    .withLogging()
                    .withConsumerConfig(
                        kafkaTopicProperties.innsatsgruppeTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.stringDeserializer(),
                        Consumer { innsatsgruppeConsumer.ingest(it.value()) },
                    ),
            ),
        ).build()

    private var running = false

    override fun start() {
        if (running) return

        log.info("Starter midlertidig replay-consumer for innsatsgruppe-topic med group-id=$replayConsumerGroupId")
        client.start()
        running = true
    }

    override fun stop() {
        if (!running) return

        log.info("Stopper midlertidig replay-consumer for innsatsgruppe-topic")
        client.stop()
        running = false
    }

    override fun isRunning() = running
}
