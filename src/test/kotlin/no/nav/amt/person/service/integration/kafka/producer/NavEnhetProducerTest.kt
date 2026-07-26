package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.verify
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class NavEnhetProducerTest(
    private val kafkaProducerService: KafkaProducerService,
) : IntegrationTestBase() {
    @Test
    fun `publiserNavEnhet - skal publisere enhet med riktig key og value`() {
        val navEnhet = TestData.lagNavEnhet()

        kafkaProducerService.publiserNavEnhet(navEnhet)

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val forventetValue = objectMapper.writeValueAsString(NavEnhetDtoV1.fromDbo(navEnhet))

        recordSlot.captured.key() shouldBe navEnhet.id.toString()
        recordSlot.captured.value() shouldBe forventetValue
    }
}
