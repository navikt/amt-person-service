package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.navansatt.NavAnsattService
import no.nav.amt.person.service.navansatt.NavAnsattUpdater
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

class NavAnsattProducerTest(
    private val kafkaProducerService: KafkaProducerService,
    private val navAnsattService: NavAnsattService,
    private val navAnsattUpdater: NavAnsattUpdater,
) : IntegrationTestBase() {
    @Test
    fun `publiserNavAnsatt - skal publisere ansatt med riktig key og value`() {
        val ansatt = TestData.lagNavAnsatt()

        kafkaProducerService.publiserNavAnsatt(ansatt)

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val forventetValue = ansattTilV1Json(ansatt)

        recordSlot.captured.key() shouldBe ansatt.id.toString()
        recordSlot.captured.value() shouldBe forventetValue
    }

    @Test
    fun `publiserNavAnsatt - ansatt er oppdatert - skal publisere ny melding`() {
        val ansatt = TestData.lagNavAnsatt()
        testDataRepository.insertNavAnsatt(ansatt)

        val oppdatertAnsatt = ansatt.copy(navn = "nytt navn", telefon = "nytt nummer", epost = "ny@epost.no")
        navAnsattService.upsert(oppdatertAnsatt)

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify { kafkaProducerClient.sendSync(capture(recordSlot)) }

        val forventetValue = ansattTilV1Json(oppdatertAnsatt)

        recordSlot.captured.key() shouldBe ansatt.id.toString()
        recordSlot.captured.value() shouldBe forventetValue
    }

    @Test
    fun `publiserNavAnsatt - flere ansatte sjekkes for oppdatering - skal publisere melding kun for de med endring`() {
        val endretAnsatt = TestData.lagNavAnsatt()
        testDataRepository.insertNavAnsatt(endretAnsatt)

        val uendretAnsatt = TestData.lagNavAnsatt()
        testDataRepository.insertNavAnsatt(uendretAnsatt)

        every { nomClient.hentNavAnsatte(any()) } returns listOf(
            NomNavAnsatt(
                navIdent = endretAnsatt.navIdent,
                navn = "nytt navn",
                telefonnummer = endretAnsatt.telefon,
                epost = endretAnsatt.epost,
                orgTilknytning = TestData.orgTilknytning,
            ),
            NomNavAnsatt(
                navIdent = uendretAnsatt.navIdent,
                navn = uendretAnsatt.navn,
                telefonnummer = uendretAnsatt.telefon,
                epost = uendretAnsatt.epost,
                orgTilknytning = TestData.orgTilknytning,
            ),
        )

        navAnsattUpdater.oppdaterAlle()

        val recordSlot = slot<ProducerRecord<String, String>>()
        verify(exactly = 1) { kafkaProducerClient.sendSync(capture(recordSlot)) }

        recordSlot.captured.key() shouldBe endretAnsatt.id.toString()
    }

    private fun ansattTilV1Json(ansatt: NavAnsattDbo): String = objectMapper.writeValueAsString(
        NavAnsattDtoV1(
            id = ansatt.id,
            navident = ansatt.navIdent,
            navn = ansatt.navn,
            telefon = ansatt.telefon,
            epost = ansatt.epost,
            navEnhetId = ansatt.navEnhetId,
        ),
    )
}
