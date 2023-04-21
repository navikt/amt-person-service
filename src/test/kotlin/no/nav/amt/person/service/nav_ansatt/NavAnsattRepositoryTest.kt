package no.nav.amt.person.service.nav_ansatt

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import no.nav.amt.person.service.utils.shouldBeCloseTo
import no.nav.amt.person.service.utils.shouldBeEqualTo
import org.junit.AfterClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*
import kotlin.NoSuchElementException

class NavAnsattRepositoryTest {
	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = NavAnsattRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@Test
	fun `get(uuid) - ansatt finnes - returnerer ansatt`() {
		val ansatt = TestData.lagNavAnsatt()
		testRepository.insertNavAnsatt(ansatt)

		val faktiskAnsatt = repository.get(ansatt.id)

		faktiskAnsatt.id shouldBe ansatt.id
		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe ansatt.navn
		faktiskAnsatt.telefon shouldBe ansatt.telefon
		faktiskAnsatt.epost shouldBe ansatt.epost
		faktiskAnsatt.createdAt shouldBeEqualTo ansatt.createdAt
		faktiskAnsatt.modifiedAt shouldBeEqualTo ansatt.modifiedAt
	}

	@Test
	fun `get(uuid) - ansatt finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			repository.get(UUID.randomUUID())
		}
	}

	@Test
	fun `get(navIdent) - ansatt finnes - returnerer ansatt`() {
		val ansatt = TestData.lagNavAnsatt()
		testRepository.insertNavAnsatt(ansatt)

		val faktiskAnsatt = repository.get(ansatt.navIdent)!!

		faktiskAnsatt.id shouldBe ansatt.id
		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe ansatt.navn
		faktiskAnsatt.telefon shouldBe ansatt.telefon
		faktiskAnsatt.epost shouldBe ansatt.epost
		faktiskAnsatt.createdAt shouldBeEqualTo ansatt.createdAt
		faktiskAnsatt.modifiedAt shouldBeEqualTo ansatt.modifiedAt
	}

	@Test
	fun `get(navIdent) - ansatt finnes ikke - returnerer null`() {
		repository.get("Ukjent Ident") shouldBe null
	}

	@Test
	fun `upsert - ansatt finnes ikke - oppretter ny ansatt`() {
		val ansatt = TestData.lagNavAnsatt().toModel()

		repository.upsert(ansatt)

		val faktiskAnsatt = repository.get(ansatt.navIdent)!!

		faktiskAnsatt.id shouldBe ansatt.id
		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe ansatt.navn
		faktiskAnsatt.telefon shouldBe ansatt.telefon
		faktiskAnsatt.epost shouldBe ansatt.epost
	}

	@Test
	fun `upsert - ansatt finnes - oppdatterer ansatt`() {
		val ansatt = TestData.lagNavAnsatt(
			createdAt = LocalDateTime.now().minusMonths(6),
			modifiedAt = LocalDateTime.now().minusMonths(6),
		)

		val oppdatertAnsatt = NavAnsatt(
			id = ansatt.id,
			navIdent = ansatt.navIdent,
			navn = "Nytt Navn",
			epost = "ny_epost@nav.no",
			telefon = "12345678",
		)

		testRepository.insertNavAnsatt(ansatt)

		repository.upsert(oppdatertAnsatt)

		val faktiskAnsatt = repository.get(ansatt.id)

		faktiskAnsatt.id shouldBe ansatt.id
		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe oppdatertAnsatt.navn
		faktiskAnsatt.telefon shouldBe oppdatertAnsatt.telefon
		faktiskAnsatt.epost shouldBe oppdatertAnsatt.epost
		faktiskAnsatt.createdAt shouldBeEqualTo ansatt.createdAt
		faktiskAnsatt.modifiedAt shouldBeCloseTo LocalDateTime.now()
	}

	@Test
	fun `getAll - ansatte finnes - returnerer liste med ansatte`() {
		val ansatt = TestData.lagNavAnsatt()
		testRepository.insertNavAnsatt(ansatt)

		val alleAnsatte = repository.getAll()

		alleAnsatte shouldHaveAtLeastSize 1
		alleAnsatte.firstOrNull{ it.id == ansatt.id } shouldNotBe null

	}

	@Test
	fun `updateMany - flere ansatte - oppdaterer ansatte`() {
		val ansatte = listOf(TestData.lagNavAnsatt(), TestData.lagNavAnsatt())
		ansatte.forEach { testRepository.insertNavAnsatt(it) }

		val oppdaterteAnsatte = listOf(
			NomNavAnsatt(
				navIdent = ansatte[0].navIdent,
				navn =	"nytt navn 1",
				telefonnummer = ansatte[0].telefon,
				epost = ansatte[0].epost
			),
			NomNavAnsatt(
				navIdent = ansatte[1].navIdent,
				navn =	"nytt navn 2",
				telefonnummer = ansatte[1].telefon,
				epost = ansatte[1].epost
			),
		)

		repository.updateMany(oppdaterteAnsatte)

		repository.get(oppdaterteAnsatte[0].navIdent)!!.navn shouldBe oppdaterteAnsatte[0].navn
		repository.get(oppdaterteAnsatte[1].navIdent)!!.navn shouldBe oppdaterteAnsatte[1].navn
	}

}
