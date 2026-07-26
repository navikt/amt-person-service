package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.slot
import io.mockk.verify
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.ArrangorAnsattDtoV1
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.Rolle
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class ArrangorAnsattProducerTest(
    private val kafkaProducerService: KafkaProducerService,
    private val personService: PersonService,
) : IntegrationTestBase() {
    @Test
    fun `publiserArrangorAnsatt - skal publisere ansatt med riktig key og value`() {
        val ansatt = TestData.lagPerson()
        kafkaProducerService.publiserArrangorAnsatt(ansatt)

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val forventetValue = objectMapper.writeValueAsString(ArrangorAnsattDtoV1.fromDbo(ansatt))

        recordSlot.captured.key() shouldBe ansatt.id.toString()
        recordSlot.captured.value() shouldBe forventetValue
    }

    @Test
    fun `publiserArrangorAnsatt - person oppdateres - skal publisere ansatt med riktig key og value`() {
        val ansatt = TestData.lagPerson()
        testDataRepository.insertPerson(ansatt)
        testDataRepository.insertRolle(ansatt.id, Rolle.ARRANGOR_ANSATT)

        val oppdatertAnsatt = ansatt.copy(fornavn = "Nytt", mellomnavn = null, etternavn = "Navn")
        personService.upsert(oppdatertAnsatt)

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val forventetValue = objectMapper.writeValueAsString(ArrangorAnsattDtoV1.fromDbo(oppdatertAnsatt))

        recordSlot.captured.key() shouldBe ansatt.id.toString()
        recordSlot.captured.value() shouldBe forventetValue
    }

    @Test
    fun `publiserArrangorAnsatt - person oppdateres, er ikke arrangor ansatt - skal ikke publiseres`() {
        val navBruker = TestData.lagPerson()
        testDataRepository.insertPerson(navBruker)
        testDataRepository.insertRolle(navBruker.id, Rolle.NAV_BRUKER)

        val oppdaterNavBruker = navBruker.copy(fornavn = "Nytt", mellomnavn = null, etternavn = "Navn")
        personService.upsert(oppdaterNavBruker)

        verify(exactly = 0) {
            kafkaProducerClient.sendSync(match { it.topic() == "amt-arrangor-ansatt-personalia-topic" })
        }
    }

    @Test
    fun `publiserArrangorAnsatt - person oppdateres, har flere roller - skal publiseres`() {
        val navBruker = TestData.lagNavBruker()
        testDataRepository.insertNavBruker(navBruker)

        val person = navBruker.person
        testDataRepository.insertRolle(person.id, Rolle.ARRANGOR_ANSATT)

        val oppdatertPerson = person.copy(fornavn = "Nytt", mellomnavn = null, etternavn = "Navn")
        personService.upsert(oppdatertPerson)

        val records = mutableListOf<ProducerRecord<String, String>>()
        verify(atLeast = 1) { kafkaProducerClient.sendSync(capture(records)) }

        records.firstOrNull { it.key() == person.id.toString() } shouldNotBe null
    }
}
