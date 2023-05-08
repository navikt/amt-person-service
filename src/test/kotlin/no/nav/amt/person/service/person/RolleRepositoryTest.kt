package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class RolleRepositoryTest {
	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = RolleRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@Test
	fun `insert - ny rolle - inserter rolle`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)
		repository.insert(person.id, Rolle.ARRANGOR_ANSATT)

		repository.harRolle(person.id, Rolle.ARRANGOR_ANSATT) shouldBe true
	}

	@Test
	fun `insert - rolle eksisterer fra før - kombinasjonen personId og rolle skal være unik`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)
		repository.insert(person.id, Rolle.ARRANGOR_ANSATT)

		assertThrows<DuplicateKeyException> {
			repository.insert(person.id, Rolle.ARRANGOR_ANSATT)
		}
	}

	@Test
	fun `delete - rolle finnes - sletter rolle for person`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)
		testRepository.insertRolle(person.id, Rolle.NAV_BRUKER)

		repository.delete(person.id, Rolle.NAV_BRUKER)

		repository.harRolle(person.id, Rolle.NAV_BRUKER) shouldBe false
	}
}
