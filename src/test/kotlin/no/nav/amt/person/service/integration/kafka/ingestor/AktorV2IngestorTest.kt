package no.nav.amt.person.service.integration.kafka.ingestor

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

			faktiskPerson!!.historiskeIdenter.contains(person.personIdent) shouldBe true
			faktiskPerson.personIdentType shouldBe IdentType.FOLKEREGISTERIDENT
		}

	}

	@Test
	fun `ingest - bruker får flere gjeldende identer - skal lagre FOLKEREGISTERIDENT`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val nyttFnr = TestData.randomIdent()
		val nyNpid = TestData.randomIdent()

		val msg = Aktor(
			listOf(
				Identifikator(nyNpid, Type.NPID, true),
				Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
				Identifikator(person.personIdent, Type.FOLKEREGISTERIDENT, false),
			)
		)

		mockSchemaRegistryHttpServer.registerSchema(1, "aktor-v2-topic", msg.schema)

		kafkaMessageSender.sendTilAktorV2Topic("aktorId", msg, 1)

		AsyncUtils.eventually {
			val faktiskPerson = personService.hentPerson(nyttFnr)

			faktiskPerson!!.historiskeIdenter.contains(person.personIdent) shouldBe true
			faktiskPerson.historiskeIdenter.contains(nyNpid) shouldBe true
			faktiskPerson.personIdentType shouldBe IdentType.FOLKEREGISTERIDENT
		}

	}

}
