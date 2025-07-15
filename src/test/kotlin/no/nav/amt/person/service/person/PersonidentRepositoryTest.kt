package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class PersonidentRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = PersonidentRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}


	@Test
	fun `upsertPersonident - nye identer - inserter`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)

		val identer = listOf(
			Personident(TestData.randomIdent(), true, IdentType.AKTORID),
			Personident(TestData.randomIdent(), true, IdentType.NPID),
		)

		repository.upsert(person.id, identer)

		val faktiskeIdenter = repository.getAllForPerson(person.id)

		faktiskeIdenter.any { it.ident == identer[0].ident } shouldBe true
		faktiskeIdenter.any { it.ident == identer[1].ident } shouldBe true
	}

	@Test
	fun `upsertPersonident - ny ident - inserter og oppdaterer`() {
		val person = TestData.lagPerson()
		testRepository.insertPerson(person)

		val identer = listOf(
			TestData.lagPersonident(person.personident, historisk = true),
			TestData.lagPersonident(TestData.randomIdent(), historisk = false),
		)

		repository.upsert(person.id, identer.map { it.toModel() })

		val faktiskeIdenter = repository.getAllForPerson(person.id)

		faktiskeIdenter.any { it.ident == identer[0].ident && it.historisk } shouldBe true
		faktiskeIdenter.any { it.ident == identer[1].ident && !it.historisk } shouldBe true
	}
}
