package no.nav.amt.person.service.kafka

import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("kafka.enabled", havingValue = "true", matchIfMissing = true)
class KafkaConsumerLifecycle(
    private val client: KafkaConsumerClient,
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(javaClass)

    private var running = false

    override fun start() {
        if (running) {
            return
        }

        log.info("Starting Kafka consumer and stored record processor...")
        client.start()
        consumerRecordProcessor.start()

        running = true
    }

    override fun stop() {
        if (!running) {
            return
        }

        log.info("Stopping Kafka consumer and stored record processor...")
        consumerRecordProcessor.stop()
        client.stop()

        running = false
    }

    override fun isRunning() = running
}
