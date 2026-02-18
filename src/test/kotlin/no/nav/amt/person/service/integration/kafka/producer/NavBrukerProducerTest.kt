package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer.consume
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.navbruker.Adressebeskyttelse
import no.nav.amt.person.service.navbruker.NavBrukerDbo
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import org.junit.jupiter.api.Test
import java.util.UUID

class NavBrukerProducerTest(
	private val kafkaProducerService: KafkaProducerService,
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) : IntegrationTestBase() {
	@Test
	fun `publiserNavBruker - skal publisere bruker med riktig key og value`() {
		val navBruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.FORTROLIG)

		kafkaProducerService.publiserNavBruker(navBruker)

		val records = consume(kafkaTopicProperties.amtNavBrukerTopic)
		records.shouldNotBeNull()
		val record = records.first { it.key() == navBruker.person.id.toString() }

		val forventetValue = brukerTilV1Json(navBruker)

		record.key() shouldBe navBruker.person.id.toString()
		record.value() shouldBe forventetValue
	}

	@Test
	fun `publiserSlettNavBruker - skal publisere tombstone med riktig key og null value`() {
		val personId = UUID.randomUUID()

		kafkaProducerService.publiserSlettNavBruker(personId)

		val records = consume(kafkaTopicProperties.amtNavBrukerTopic)
		records.shouldNotBeNull()
		val record = records.first { it.key() == personId.toString() }

		record.value() shouldBe null
	}

	@Test
	fun `personService upsert - bruker finnes - produserer melding`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val oppdatertBruker = bruker.copy(person = bruker.person.copy(fornavn = "Nytt Navn"))
		personService.upsert(oppdatertBruker.person)

		val records = consume(kafkaTopicProperties.amtNavBrukerTopic)
		records.shouldNotBeNull()
		val record = records.first { it.key() == bruker.person.id.toString() }

		record.value() shouldBe brukerTilV1Json(oppdatertBruker)
	}

	@Test
	fun `navBrukerService upsert - bruker finnes - produserer melding`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val oppdatertBruker = bruker.copy(navEnhet = null)
		navBrukerService.upsert(oppdatertBruker)

		val records = consume(kafkaTopicProperties.amtNavBrukerTopic)
		records.shouldNotBeNull()
		val record = records.first { it.key() == bruker.person.id.toString() }

		record.value() shouldBe brukerTilV1Json(oppdatertBruker)
	}

	private fun brukerTilV1Json(navBruker: NavBrukerDbo): String =
		objectMapper.writeValueAsString(
			NavBrukerDtoV1(
				personId = navBruker.person.id,
				personident = navBruker.person.personident,
				fornavn = navBruker.person.fornavn,
				mellomnavn = navBruker.person.mellomnavn,
				etternavn = navBruker.person.etternavn,
				navVeilederId = navBruker.navVeileder?.id,
				navEnhet = navBruker.navEnhet?.let { NavEnhetDtoV1(it.id, it.enhetId, it.navn) },
				telefon = navBruker.telefon,
				epost = navBruker.epost,
				erSkjermet = navBruker.erSkjermet,
				adresse = navBruker.adresse,
				adressebeskyttelse = navBruker.adressebeskyttelse,
				oppfolgingsperioder = navBruker.oppfolgingsperioder,
				innsatsgruppe = navBruker.innsatsgruppe,
			),
		)
}
