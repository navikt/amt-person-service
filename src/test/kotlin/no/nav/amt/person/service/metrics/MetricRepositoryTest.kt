package no.nav.amt.person.service.metrics

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class MetricRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = MetricRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}


	@Test
	fun `getCounts - teller opp ulike typer`() {
		testRepository.insertNavBruker(TestData.lagNavBruker())

		testRepository.insertPerson(TestData.lagPerson())

		testRepository.insertNavAnsatt(TestData.lagNavAnsatt())

		val counts = repository.getCounts()
		counts.antallNavBrukere shouldBe 1
		counts.antallPersoner shouldBe 2
		counts.antallNavAnsatte shouldBe 2
		counts.antallNavEnheter shouldBe 1
		counts.antallArrangorAnsatte shouldBe 0
	}

}
