package no.nav.amt.person.service.person

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import no.nav.amt.person.service.utils.shouldBeCloseTo
import no.nav.amt.person.service.utils.shouldBeEqualTo
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

class PersonRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = PersonRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@Test
	fun `get(uuid) - person finnes - returnerer person`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)

		val faktiskPerson = repository.get(person.id)

		faktiskPerson.id shouldBe person.id
		faktiskPerson.personIdent shouldBe  person.personIdent
		faktiskPerson.personIdentType shouldBe person.personIdentType
		faktiskPerson.historiskeIdenter shouldBe person.historiskeIdenter
		faktiskPerson.fornavn shouldBe person.fornavn
		faktiskPerson.mellomnavn shouldBe person.mellomnavn
		faktiskPerson.etternavn shouldBe person.etternavn
		faktiskPerson.createdAt shouldBeEqualTo person.createdAt
		faktiskPerson.modifiedAt shouldBeEqualTo person.modifiedAt
	}

	@Test
	fun `get(uuid) - person finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			repository.get(UUID.randomUUID())
		}
	}

	@Test
	fun `get(personIdent) - person finnes ikke - returnerer null`() {
		repository.get(TestData.randomIdent()) shouldBe null
	}

	@Test
	fun `getPersoner - flere personer, en finnes ikke - returnerer personer som finnes fra før`() {
		val person1 = TestData.lagPerson()
		val person2 = TestData.lagPerson()
		testRepository.insertPerson(person1)
		testRepository.insertPerson(person2)

		val personer = repository.getPersoner(listOf(
			person1.personIdent,
			person2.personIdent,
			TestData.randomIdent()
		))

		personer shouldHaveSize 2
		personer.any { it.id == person1.id } shouldBe true
		personer.any { it.id == person2.id } shouldBe true
	}

	@Test
	fun `getPersoner - person med historisk ident - returnerer liste`() {
		val historiskIdent = TestData.randomIdent()
		val person = TestData.lagPerson(historiskeIdenter = listOf(historiskIdent))

		testRepository.insertPerson(person)

		val faktiskPerson = repository.getPersoner(listOf(historiskIdent)).first()

		faktiskPerson.id shouldBe  person.id
		faktiskPerson.historiskeIdenter.contains(historiskIdent) shouldBe true
		faktiskPerson.personIdent shouldNotBe historiskIdent
	}

	@Test
	fun `getPersoner - ingen person med ident - returnerer tom liste`() {
		repository.getPersoner(listOf(TestData.randomIdent(), TestData.randomIdent())) shouldBe emptyList()
	}


	@Test
	fun `upsert - ny person - inserter person`() {
		val person = Person(
			id = UUID.randomUUID(),
			personIdent = TestData.randomIdent(),
			personIdentType = IdentType.FOLKEREGISTERIDENT,
			historiskeIdenter = listOf(TestData.randomIdent()),
			fornavn = "Fornavn",
			mellomnavn = "Mellomnavn",
			etternavn = "Etternavn",
		)

		repository.upsert(person)

		val faktiskPerson = repository.get(person.id)

		faktiskPerson.id shouldBe person.id
		faktiskPerson.personIdent shouldBe  person.personIdent
		faktiskPerson.personIdentType shouldBe person.personIdentType
		faktiskPerson.historiskeIdenter shouldBe person.historiskeIdenter
		faktiskPerson.fornavn shouldBe person.fornavn
		faktiskPerson.mellomnavn shouldBe person.mellomnavn
		faktiskPerson.etternavn shouldBe person.etternavn
	}

	@Test
	fun `upsert - eksisterende person - oppdaterer person`() {
		val originalPerson = TestData.lagPerson(
			createdAt = LocalDateTime.now().minusMonths(6),
			modifiedAt = LocalDateTime.now().minusMonths(6),
		)

		testRepository.insertPerson(originalPerson)

		val oppdatertPerson = Person(
				id = originalPerson.id,
				personIdent = originalPerson.personIdent,
				personIdentType = originalPerson.personIdentType,
				fornavn = "Nytt",
				mellomnavn = "Navn",
				etternavn = "Med Mer",
				historiskeIdenter = originalPerson.historiskeIdenter.plus(TestData.randomIdent())
			)

		repository.upsert(oppdatertPerson)

		val faktiskPerson = repository.get(originalPerson.id)

		faktiskPerson.id shouldBe originalPerson.id
		faktiskPerson.personIdent shouldBe  originalPerson.personIdent
		faktiskPerson.personIdentType shouldBe originalPerson.personIdentType
		faktiskPerson.historiskeIdenter shouldBe oppdatertPerson.historiskeIdenter
		faktiskPerson.fornavn shouldBe oppdatertPerson.fornavn
		faktiskPerson.mellomnavn shouldBe oppdatertPerson.mellomnavn
		faktiskPerson.etternavn shouldBe oppdatertPerson.etternavn
		faktiskPerson.createdAt shouldBeEqualTo originalPerson.createdAt
		faktiskPerson.modifiedAt shouldBeCloseTo LocalDateTime.now()
	}

	@Test
	fun `upsert - ny ident - oppdaterer person`() {
		val originalPerson = TestData.lagPerson(
			createdAt = LocalDateTime.now().minusMonths(6),
			modifiedAt = LocalDateTime.now().minusMonths(6),
		)

		testRepository.insertPerson(originalPerson)

		val oppdatertPerson = Person(
			id = originalPerson.id,
			personIdent = "ny ident",
			personIdentType = originalPerson.personIdentType,
			fornavn = "Nytt",
			mellomnavn = "Navn",
			etternavn = "Med Mer",
			historiskeIdenter = originalPerson.historiskeIdenter.plus(originalPerson.personIdent)
		)

		repository.upsert(oppdatertPerson)

		val faktiskPerson = repository.get(originalPerson.id)

		faktiskPerson.id shouldBe originalPerson.id
		faktiskPerson.personIdent shouldBe  oppdatertPerson.personIdent
		faktiskPerson.personIdentType shouldBe oppdatertPerson.personIdentType
		faktiskPerson.historiskeIdenter shouldBe oppdatertPerson.historiskeIdenter
		faktiskPerson.fornavn shouldBe oppdatertPerson.fornavn
		faktiskPerson.mellomnavn shouldBe oppdatertPerson.mellomnavn
		faktiskPerson.etternavn shouldBe oppdatertPerson.etternavn
		faktiskPerson.createdAt shouldBeEqualTo originalPerson.createdAt
		faktiskPerson.modifiedAt shouldBeCloseTo LocalDateTime.now()
	}

	@Test
	fun `delete - person finnes - sletter person()`(){
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)

		repository.delete(person.id)

		repository.get(person.personIdent) shouldBe null
	}

}
