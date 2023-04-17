package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.person.dbo.PersonUpsert
import no.nav.amt.person.service.person.model.IdentType
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
	fun `get - person finnes - returnerer person`() {
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
	fun `get - person finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			repository.get(UUID.randomUUID())
		}
	}

	@Test
	fun `upsert - ny person - inserter person`() {
		val person = PersonUpsert(
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

		val oppdatertPerson = PersonUpsert(
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

}
