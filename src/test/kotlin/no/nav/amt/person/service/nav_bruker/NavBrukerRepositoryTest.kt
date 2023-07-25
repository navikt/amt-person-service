package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
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

class NavBrukerRepositoryTest {

	companion object {
		private val dataSource = SingletonPostgresContainer.getDataSource()
		private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		val testRepository = TestDataRepository(jdbcTemplate)
		val repository = NavBrukerRepository(jdbcTemplate)

		@JvmStatic
		@AfterClass
		fun tearDown() {
			DbTestDataUtils.cleanDatabase(dataSource)
		}
	}

	@Test
	fun `get(uuid) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val faktiskBruker = repository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(uuid) - bruker finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			repository.get(UUID.randomUUID())
		}
	}

	@Test
	fun `get(personIdent) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val faktiskBruker = repository.get(bruker.person.personIdent)!!

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(personIdent) - søk med historisk ident, bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val historiskIdent = TestData.lagPersonident(personId = bruker.person.id, historisk = true)
		testRepository.insertPersonidenter(listOf(historiskIdent))

		val faktiskBruker = repository.get(historiskIdent.ident)!!

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(personIdent) - bruker finnes ikke - returnerer null`() {
		repository.get("FNR") shouldBe null
	}

	@Test
	fun `upsert - bruker finnes ikke - inserter ny bruker`() {
		val bruker = TestData.lagNavBruker()

		testRepository.insertPerson(bruker.person)
		testRepository.insertNavAnsatt(bruker.navVeileder!!)
		testRepository.insertNavEnhet(bruker.navEnhet!!)

		repository.upsert(NavBrukerUpsert(
			bruker.id,
			bruker.person.id,
			bruker.navVeileder?.id,
			bruker.navEnhet?.id,
			bruker.telefon,
			bruker.epost,
			bruker.erSkjermet
		))

		val faktiskBruker = repository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `upsert - bruker finnes - oppdaterer bruker`() {
		val bruker = TestData.lagNavBruker(
			createdAt = LocalDateTime.now().minusMonths(6),
			modifiedAt = LocalDateTime.now().minusMonths(6),
			erSkjermet = false,
		)
		testRepository.insertNavBruker(bruker)

		repository.get(bruker.id)

		val upsert = NavBrukerUpsert(
			id = bruker.id,
			personId = bruker.person.id,
			navVeilederId = null,
			navEnhetId = null,
			telefon = "ny telefon",
			epost = "ny@epost.no",
			erSkjermet = true
		)

		repository.upsert(upsert)

		val faktiskBruker = repository.get(bruker.id)

		faktiskBruker.navVeileder shouldBe null
		faktiskBruker.navEnhet shouldBe null
		faktiskBruker.erSkjermet shouldBe true

		faktiskBruker.telefon shouldBe upsert.telefon
		faktiskBruker.epost shouldBe upsert.epost

		faktiskBruker.createdAt shouldBeEqualTo bruker.createdAt
		faktiskBruker.modifiedAt shouldBeCloseTo LocalDateTime.now()

	}

	@Test
	fun `finnBrukerId - bruker finnes ikke - returnerer null`() {
		repository.finnBrukerId("en ident") shouldBe null
	}

	@Test
	fun `finnBrukerId - bruker finnes - returnerer id`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		repository.finnBrukerId(bruker.person.personIdent) shouldBe bruker.id
	}

	@Test
	fun `finnBrukerId - søk med historisk ident, bruker finnes - returnerer id`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)
		val historiskIdent = TestData.lagPersonident(personId = bruker.person.id, historisk = true)
		testRepository.insertPersonidenter(listOf(historiskIdent))

		repository.finnBrukerId(historiskIdent.ident) shouldBe bruker.id
	}

	@Test
	fun `delete- bruker finnes - sletter bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		repository.delete(bruker.id)

		repository.get(bruker.person.personIdent) shouldBe null
	}

	private fun sammenlign(
		faktiskBruker: NavBrukerDbo,
		bruker: NavBrukerDbo,
	) {
		faktiskBruker.id shouldBe bruker.id
		faktiskBruker.person.id shouldBe bruker.person.id
		faktiskBruker.navVeileder?.id shouldBe bruker.navVeileder?.id
		faktiskBruker.navEnhet?.id shouldBe bruker.navEnhet?.id
		faktiskBruker.telefon shouldBe bruker.telefon
		faktiskBruker.epost shouldBe bruker.epost
		faktiskBruker.erSkjermet shouldBe bruker.erSkjermet
		faktiskBruker.createdAt shouldBeCloseTo bruker.createdAt
		faktiskBruker.modifiedAt shouldBeCloseTo bruker.modifiedAt
	}

}
