package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.verify
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.kafka.consumer.AktorV2Consumer
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonidentRepository
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class AktorV2ConsumerTest(
    private val aktorV2Consumer: AktorV2Consumer,
    private val personidentRepository: PersonidentRepository,
    private val personRepository: PersonRepository,
) : IntegrationTestBase() {
    @Test
    fun `ingest - ny person ident - oppdaterer person`() {
        val navBruker = TestData.lagNavBruker()
        testDataRepository.insertNavBruker(navBruker)
        val person = navBruker.person

        val nyttFnr = TestData.randomIdent()

        val msg = Aktor(
            listOf(
                Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
                Identifikator(person.personident, Type.FOLKEREGISTERIDENT, false),
            ),
        )

        aktorV2Consumer.ingest("aktorId", msg)

        val faktiskPerson = personRepository.get(nyttFnr).shouldNotBeNull()

        val identer = personidentRepository.getAllForPerson(faktiskPerson.id)

        assertSoftly(identer.first { it.ident == person.personident }) {
            it.historisk shouldBe true
            it.type shouldBe IdentType.FOLKEREGISTERIDENT
        }

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val navBrukerRecord = staticObjectMapper.readValue(recordSlot.captured.value(), NavBrukerDtoV1::class.java)
        recordSlot.captured.key() shouldBe person.id.toString()
        navBrukerRecord.personident shouldBe nyttFnr
    }

    @Test
    fun `ingest - bruker far flere gjeldende identer - skal lagre FOLKEREGISTERIDENT`() {
        val person = TestData.lagPerson()
        testDataRepository.insertPerson(person)

        val nyttFnr = TestData.randomIdent()
        val aktorId = TestData.randomIdent()

        val msg = Aktor(
            listOf(
                Identifikator(aktorId, Type.AKTORID, true),
                Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
                Identifikator(person.personident, Type.FOLKEREGISTERIDENT, false),
            ),
        )

        aktorV2Consumer.ingest("aktorId", msg)

        val faktiskPerson = personRepository.get(nyttFnr).shouldNotBeNull()
        faktiskPerson.personident shouldBe nyttFnr

        val identer = personidentRepository.getAllForPerson(faktiskPerson.id)

        identer shouldHaveSize 3
        identer.first { it.ident == person.personident }.historisk shouldBe true
    }
}
