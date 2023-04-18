package no.nav.amt.person.service.nav_enhet

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
import java.util.*
import kotlin.NoSuchElementException

class NavEnhetRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = NavEnhetRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@Test
	fun `get(uuid) - enhet finnes - returnerer enhet`() {
		val enhet = TestData.lagNavEnhet()
		testRepository.insertNavEnhet(enhet)

		val faktiskEnhet = repository.get(enhet.id)

		faktiskEnhet.id shouldBe enhet.id
		faktiskEnhet.enhetId shouldBe enhet.enhetId
		faktiskEnhet.navn shouldBe enhet.navn
		faktiskEnhet.createdAt shouldBeEqualTo enhet.createdAt
		faktiskEnhet.modifiedAt shouldBeEqualTo enhet.modifiedAt
	}

	@Test
	fun `get(uuid) - enhet finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			repository.get(UUID.randomUUID())
		}
	}


	@Test
	fun `get(enhetId) - enhet finnes - returnerer enhet`() {
		val enhet = TestData.lagNavEnhet()
		testRepository.insertNavEnhet(enhet)

		val faktiskEnhet = repository.get(enhet.enhetId)!!

		faktiskEnhet.id shouldBe enhet.id
		faktiskEnhet.enhetId shouldBe enhet.enhetId
		faktiskEnhet.navn shouldBe enhet.navn
		faktiskEnhet.createdAt shouldBeEqualTo enhet.createdAt
		faktiskEnhet.modifiedAt shouldBeEqualTo enhet.modifiedAt
	}

	@Test
	fun `get(enhetId) - enhet finnes ikke - returnerer null`() {
		repository.get("Ikke Eksisternede Enhet") shouldBe null
	}

	@Test
	fun `insert - ny enhet - inserter ny enhet`() {
		val enhet = NavEnhet(
			id = UUID.randomUUID(),
			enhetId = "0001",
			navn = "Ny Nav Enhet",
		)

		repository.insert(enhet)

		val faktiskEnhet = repository.get(enhet.id)

		faktiskEnhet.id shouldBe enhet.id
		faktiskEnhet.enhetId shouldBe enhet.enhetId
		faktiskEnhet.navn shouldBe enhet.navn
	}

}
