package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerKontaktinfo
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
import java.util.*

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
	fun `oppdaterNavVeileder - bruker og veileder finnes - oppdaterer veilderId`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)
		val veileder = TestData.lagNavAnsatt()
		testRepository.insertNavAnsatt(veileder)

		repository.oppdaterNavVeileder(bruker.id, veileder.id)

		val faktiskBruker = repository.get(bruker.id)

		faktiskBruker.navVeileder?.id shouldBe veileder.id
		faktiskBruker.modifiedAt shouldBeCloseTo LocalDateTime.now()
	}

	@Test
	fun `settSkjermet - bruker er ikke skjermet - oppdaterer erSkjermet`() {
		val bruker = TestData.lagNavBruker(erSkjermet = false)
		testRepository.insertNavBruker(bruker)

		repository.settSkjermet(bruker.id, true)

		val faktiskBruker = repository.get(bruker.id)

		faktiskBruker.erSkjermet shouldBe true
		faktiskBruker.modifiedAt shouldBeCloseTo LocalDateTime.now()

	}

	@Test
	fun `oppdaterKontakinformasjon - ny kontaktinfo - oppdaterer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val nyttNummer = "nytt telefonnummer"
		val nyEpost = "ny@epost.dev"

		repository.oppdaterKontaktinformasjon(NavBrukerKontaktinfo(bruker.id, nyttNummer, nyEpost))

		val faktiskBruker = repository.get(bruker.id)
		faktiskBruker.telefon shouldBe nyttNummer
		faktiskBruker.epost shouldBe nyEpost
	}

	@Test
	fun `deleteByPersonId - bruker finnes - sletter bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		repository.deleteByPersonId(bruker.person.id)

		repository.get(bruker.person.personIdent) shouldBe null
	}

	@Test
	fun `finnKontaktinformasjon - bruker finnes - returnerer kontaktinfo`() {
		val bruker = TestData.lagNavBruker(epost = null)
		testRepository.insertNavBruker(bruker)

		val kontaktinfo = repository.finnKontaktinformasjon(bruker.person.personIdent)
		kontaktinfo!!.navBrukerId shouldBe bruker.id
		kontaktinfo.telefon shouldBe bruker.telefon
		kontaktinfo.epost shouldBe bruker.epost
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
		faktiskBruker.createdAt shouldBeEqualTo bruker.createdAt
		faktiskBruker.modifiedAt shouldBeEqualTo bruker.modifiedAt
	}

}
