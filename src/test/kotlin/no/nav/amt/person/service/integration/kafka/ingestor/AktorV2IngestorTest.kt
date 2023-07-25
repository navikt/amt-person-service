package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AktorV2IngestorTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var personService: PersonService

	@Test
	fun `ingest - ny person ident - oppdaterer person`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val nyttFnr = TestData.randomIdent()

		val msg = Aktor(
			listOf(
				Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
				Identifikator(person.personIdent, Type.FOLKEREGISTERIDENT, false),
			)
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "aktor-v2-topic", msg.schema)

		kafkaMessageSender.sendTilAktorV2Topic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val faktiskPerson = personService.hentPerson(nyttFnr)

			val identer = personService.hentIdenter(faktiskPerson!!.id)

			identer.find { it.ident == person.personIdent }!!.let {
				it.historisk shouldBe true
				it.type shouldBe IdentType.FOLKEREGISTERIDENT
			}
		}

	}

	@Test
	fun `ingest - bruker får flere gjeldende identer - skal lagre FOLKEREGISTERIDENT`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val nyttFnr = TestData.randomIdent()
		val aktorId = TestData.randomIdent()

		val msg = Aktor(
			listOf(
				Identifikator(aktorId, Type.AKTORID, true),
				Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
				Identifikator(person.personIdent, Type.FOLKEREGISTERIDENT, false),
			)
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "aktor-v2-topic", msg.schema)

		kafkaMessageSender.sendTilAktorV2Topic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val faktiskPerson = personService.hentPerson(nyttFnr)

			faktiskPerson!!.personIdent shouldBe nyttFnr

			val identer = personService.hentIdenter(faktiskPerson.id)

			identer shouldHaveSize 3

			identer.find { it.ident == person.personIdent }!!.historisk shouldBe true
		}

	}

}
