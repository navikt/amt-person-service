package no.nav.amt.person.service.integration.kafka.producer

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer.consume
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.nav_bruker.Adressebeskyttelse
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class NavBrukerProducerTest: IntegrationTestBase() {

	@Autowired
	lateinit var kafkaProducerService: KafkaProducerService

	@Autowired
	lateinit var kafkaTopicProperties: KafkaTopicProperties

	@Autowired
	lateinit var personService: PersonService

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Test
	fun `publiserNavBruker - skal publisere bruker med riktig key og value`() {
		val navBruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.FORTROLIG).toModel()

		kafkaProducerService.publiserNavBruker(navBruker)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == navBruker.person.id.toString() }

		val forventetValue = brukerTilV1Json(navBruker)

		record.key() shouldBe navBruker.person.id.toString()
		record.value() shouldBe forventetValue
	}


	@Test
	fun `publiserSlettNavBruker - skal publisere tombstone med riktig key og null value`() {
		val personId = UUID.randomUUID()

		kafkaProducerService.publiserSlettNavBruker(personId)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == personId.toString() }

		record.value() shouldBe null
	}

	@Test
	fun `personService upsert - bruker finnes - produserer melding`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val oppdatertBruker = bruker.copy(person = bruker.person.copy(fornavn = "Nytt Navn")).toModel()
		personService.upsert(oppdatertBruker.person)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == bruker.person.id.toString()}

		record.value() shouldBe brukerTilV1Json(oppdatertBruker)
	}

	@Test
	fun `navBrukerService upsert - bruker finnes - produserer melding`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val oppdatertBruker = bruker.copy(navEnhet = null).toModel()
		navBrukerService.upsert(oppdatertBruker)

		val record = consume(kafkaTopicProperties.amtNavBrukerTopic)!!.first { it.key() == bruker.person.id.toString()}

		record.value() shouldBe brukerTilV1Json(oppdatertBruker)
	}

	private fun brukerTilV1Json(navBruker: NavBruker): String {
		return JsonUtils.toJsonString(
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
				adressebeskyttelse = navBruker.adressebeskyttelse
			)
		)
	}
}
