package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.navbruker.Adressebeskyttelse
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.utils.titlecase
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class LeesahConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val personRepository: PersonRepository,
	private val navBrukerRepository: NavBrukerRepository,
) : IntegrationTestBase() {
	@Test
	fun `Ingest - nav bruker finnes - oppdaterer navn`() {
		val person = TestData.lagPerson()
		val navBruker = TestData.lagNavBruker(person = person)

		testDataRepository.insertNavBruker(navBruker)

		val nyttFornavn = "NYTT FORNAVN"
		val nyttMellomnavn = "NYTT MELLOMNAVN"
		val nyttEtternavn = "NYTT ETTERNAVN"

		mockPdlHttpServer.mockHentTelefon(person.personident, null)

		mockPdlHttpServer.mockHentPerson(
			person.copy(
				fornavn = nyttFornavn,
				mellomnavn = nyttMellomnavn,
				etternavn = nyttEtternavn,
			),
		)

		val msg =
			KafkaMessageCreator.lagPersonhendelseNavn(
				personidenter = listOf(person.personident),
				fornavn = nyttFornavn,
				mellomnavn = nyttMellomnavn,
				etternavn = nyttEtternavn,
			)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		await().untilAsserted {
			assertSoftly(personRepository.get(person.id)) {
				fornavn shouldBe nyttFornavn.titlecase()
				mellomnavn shouldBe nyttMellomnavn.titlecase()
				etternavn shouldBe nyttEtternavn.titlecase()
			}
		}
	}

	@Test
	fun `Ingest - person far adressebeskyttelse - oppdaterer navbruker`() {
		val navBruker = TestData.lagNavBruker(adresse = TestData.lagAdresse())
		testDataRepository.insertNavBruker(navBruker)

		mockPdlHttpServer.mockHentPerson(
			navBruker.person.personident,
			TestData.lagPdlPerson(
				navBruker.person,
				adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
				adresse = navBruker.adresse,
			),
		)

		val msg =
			KafkaMessageCreator.lagPersonhendelseAdressebeskyttelse(
				personidenter = listOf(navBruker.person.personident),
				gradering = Gradering.STRENGT_FORTROLIG,
			)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		await().untilAsserted {
			val oppdatertNavBruker = navBrukerRepository.get(navBruker.person.personident)

			oppdatertNavBruker?.adressebeskyttelse shouldBe Adressebeskyttelse.STRENGT_FORTROLIG
			oppdatertNavBruker?.adresse shouldBe null
		}
	}
}
