package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.integration.mock.servers.MockKontaktinformasjon
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.AsyncUtils
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
	internal fun `Ingest - nav bruker finnes - oppdaterer navn og kontakinformasjon`() {
		val person = TestData.lagPerson()
		val navBruker = TestData.lagNavBruker(person = person)

		testDataRepository.insertNavBruker(navBruker)

		val nyttFornavn = "NYTT FORNAVN"
		val nyttMellomnavn = "NYTT MELLOMNAVN"
		val nyttEtternavn = "NYTT ETTERNAVN"

		val nyEpost = "ny@epost.no"
		val nyTelefon = "+12345678"

		mockKrrProxyHttpServer.mockHentKontaktinformasjon(MockKontaktinformasjon(nyEpost, nyTelefon))

		val msg = KafkaMessageCreator.lagPersonhendelseNavn(
			personIdenter = listOf(person.personIdent),
			fornavn = nyttFornavn,
			mellomnavn = nyttMellomnavn,
			etternavn = nyttEtternavn,
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val faktiskPerson = personService.hentPerson(person.id)

			faktiskPerson.fornavn shouldBe nyttFornavn
			faktiskPerson.mellomnavn shouldBe nyttMellomnavn
			faktiskPerson.etternavn shouldBe nyttEtternavn

			val faktiskNavBruker = navBrukerService.hentNavBruker(navBruker.id)
			faktiskNavBruker.epost shouldBe nyEpost
			faktiskNavBruker.telefon shouldBe nyTelefon
		}

	}
	@Test
	internal fun `Ingest - person får adressebeskyttelse - sletter nav bruker og person`() {
		val navBruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(navBruker)

		val msg = KafkaMessageCreator.lagPersonhendelseAdressebeskyttelse(
			personIdenter = listOf(navBruker.person.personIdent),
			gradering = Gradering.STRENGT_FORTROLIG,
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "leesah-topic", msg.schema)

		kafkaMessageSender.sendTilLeesahTopic("aktorId", msg, 1)

		AsyncUtils.eventually {
			navBrukerService.hentNavBruker(navBruker.person.personIdent) shouldBe null
			personService.hentPerson(navBruker.person.personIdent) shouldBe null
		}

	}


}
