package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.nav_bruker.Adressebeskyttelse
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.amt.person.service.utils.titlecase
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LeesahIngestorTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var personService: PersonService

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Test
	internal fun `Ingest - nav bruker finnes - oppdaterer navn`() {
		val person = TestData.lagPerson()
		val navBruker = TestData.lagNavBruker(person = person)

		testDataRepository.insertNavBruker(navBruker)

		val nyttFornavn = "NYTT FORNAVN"
		val nyttMellomnavn = "NYTT MELLOMNAVN"
		val nyttEtternavn = "NYTT ETTERNAVN"

		mockPdlHttpServer.mockHentTelefon(person.personident, null)

		mockPdlHttpServer.mockHentPerson(person.copy(
			fornavn = nyttFornavn,
			mellomnavn = nyttMellomnavn,
			etternavn = nyttEtternavn
		))

		val msg = KafkaMessageCreator.lagPersonhendelseNavn(
			personidenter = listOf(person.personident),
			fornavn = nyttFornavn,
			mellomnavn = nyttMellomnavn,
			etternavn = nyttEtternavn,
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val faktiskPerson = personService.hentPerson(person.id)

			faktiskPerson.fornavn shouldBe nyttFornavn.titlecase()
			faktiskPerson.mellomnavn shouldBe nyttMellomnavn.titlecase()
			faktiskPerson.etternavn shouldBe nyttEtternavn.titlecase()
		}

	}
	@Test
	internal fun `Ingest - person får adressebeskyttelse - oppdaterer navbruker`() {
		val navBruker = TestData.lagNavBruker(adresse = TestData.lagAdresse())
		testDataRepository.insertNavBruker(navBruker)

		mockPdlHttpServer.mockHentPerson(navBruker.person.personident, TestData.lagPdlPerson(
			navBruker.person,
			adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
			adresse = navBruker.adresse
		))

		val msg = KafkaMessageCreator.lagPersonhendelseAdressebeskyttelse(
			personidenter = listOf(navBruker.person.personident),
			gradering = Gradering.STRENGT_FORTROLIG,
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val oppdatertNavBruker = navBrukerService.hentNavBruker(navBruker.person.personident)

			oppdatertNavBruker?.adressebeskyttelse shouldBe Adressebeskyttelse.STRENGT_FORTROLIG
			oppdatertNavBruker?.adresse shouldBe null
		}
	}
}
