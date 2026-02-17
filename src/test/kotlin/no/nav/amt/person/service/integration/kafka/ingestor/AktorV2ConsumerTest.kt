package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageConsumer
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonidentRepository
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class AktorV2ConsumerTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val personidentRepository: PersonidentRepository,
	private val personRepository: PersonRepository,
	private val kafkaTopicProperties: KafkaTopicProperties,
) : IntegrationTestBase() {
	@Test
	fun `ingest - ny person ident - oppdaterer person`() {
		val navBruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(navBruker)
		val person = navBruker.person

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
			val faktiskPerson = personRepository.get(nyttFnr).shouldNotBeNull()

			val identer = personidentRepository.getAllForPerson(faktiskPerson.id)

			assertSoftly(identer.first { it.ident == person.personident }) {
				it.historisk shouldBe true
				it.type shouldBe IdentType.FOLKEREGISTERIDENT
			}

			assertNavBrukerProducedMedNyIdent(person, nyttFnr)
		}
	}

	@Test
	fun `ingest - bruker far flere gjeldende identer - skal lagre FOLKEREGISTERIDENT`() {
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
			val faktiskPerson = personRepository.get(nyttFnr).shouldNotBeNull()
			faktiskPerson.personident shouldBe nyttFnr

			val identer = personidentRepository.getAllForPerson(faktiskPerson.id)

			identer shouldHaveSize 3
			identer.first { it.ident == person.personident }.historisk shouldBe true
		}
	}

	private fun assertNavBrukerProducedMedNyIdent(
		person: PersonDbo,
		nyttFnr: String,
	) {
		val navbrukerRecords = KafkaMessageConsumer.consume(kafkaTopicProperties.amtNavBrukerTopic)
		navbrukerRecords.shouldNotBeNull()
		val navBrukerRecord =
			objectMapper.readValue<NavBrukerDtoV1>(
				navbrukerRecords.first { it.key() == person.id.toString() }.value(),
			)
		navBrukerRecord.personident shouldBe nyttFnr
	}
}
