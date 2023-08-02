package no.nav.amt.person.service.synchronization

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import org.junit.AfterClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class SynchronizationRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val repository = SynchronizationRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@AfterEach
	fun after() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `get(uuid) - record for sync finnes - returnerer record`() {
		val bruker = TestData.lagNavBruker()
		val toUpsert = SynchronizationUpsert(
			dataProvider = DataProvider.KRR,
			tableName = "nav_bruker",
			rowId = bruker.id
		)

		repository.upsert(toUpsert)
		val inserted = repository.get(bruker.id)

		inserted!!.rowId shouldBe toUpsert.rowId
		inserted.dataProvider shouldBe toUpsert.dataProvider
		inserted.tableName shouldBe toUpsert.tableName

	}

	@Test
	fun `delete(uuid) - record finnes - skal slette record`() {
		val bruker = TestData.lagNavBruker()
		val toUpsert = SynchronizationUpsert(
			dataProvider = DataProvider.KRR,
			tableName = "nav_bruker",
			rowId = bruker.id
		)

		repository.upsert(toUpsert)
		repository.delete(toUpsert.rowId)
		repository.get(toUpsert.rowId) shouldBe null

	}

}
