package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.IdentType
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test

class AktorV2ConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val personService: PersonService,
) : IntegrationTestBase() {
	@Test
	fun `ingest - ny person ident - oppdaterer person`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val nyttFnr = TestData.randomIdent()

		val msg =
			Aktor(
				listOf(
					Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
					Identifikator(person.personident, Type.FOLKEREGISTERIDENT, false),
				),
			)

		mockSchemaRegistryHttpServer.registerSchema(1, "aktor-v2-topic", msg.schema)

		kafkaMessageSender.sendTilAktorV2Topic("aktorId", msg, 1)

		await().untilAsserted {
			val faktiskPerson = personService.hentPerson(nyttFnr)

			faktiskPerson.shouldNotBeNull()
			val identer = personService.hentIdenter(faktiskPerson.id)

			identer.first { it.ident == person.personident }.let {
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

		val msg =
			Aktor(
				listOf(
					Identifikator(aktorId, Type.AKTORID, true),
					Identifikator(nyttFnr, Type.FOLKEREGISTERIDENT, true),
					Identifikator(person.personident, Type.FOLKEREGISTERIDENT, false),
				),
			)

		mockSchemaRegistryHttpServer.registerSchema(1, "aktor-v2-topic", msg.schema)

		kafkaMessageSender.sendTilAktorV2Topic("aktorId", msg, 1)

		await().untilAsserted {
			val faktiskPerson = personService.hentPerson(nyttFnr)

			faktiskPerson.shouldNotBeNull()
			faktiskPerson.personident shouldBe nyttFnr

			val identer = personService.hentIdenter(faktiskPerson.id)

			identer shouldHaveSize 3

			identer.first { it.ident == person.personident }.historisk shouldBe true
		}
	}
}
