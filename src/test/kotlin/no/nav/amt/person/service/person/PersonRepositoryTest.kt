package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import no.nav.amt.person.service.utils.shouldBeEqualTo
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class PersonRepositoryTest {

	companion object {
		val dataSource = SingletonPostgresContainer.getDataSource()
		val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
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
}