package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestDataRepository
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Kontaktadresse
import no.nav.amt.person.service.person.model.Vegadresse
import no.nav.amt.person.service.utils.DbTestDataUtils
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import no.nav.amt.person.service.utils.shouldBeCloseTo
import no.nav.amt.person.service.utils.shouldBeEqualTo
import org.junit.AfterClass
import org.junit.jupiter.api.AfterEach
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

	@AfterEach
	fun after() {
		DbTestDataUtils.cleanDatabase(dataSource)
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
	fun `get(personident) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val faktiskBruker = repository.get(bruker.person.personident)!!

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(personident) - søk med historisk ident, bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val historiskIdent = TestData.lagPersonident(personId = bruker.person.id, historisk = true)
		testRepository.insertPersonidenter(listOf(historiskIdent))

		val faktiskBruker = repository.get(historiskIdent.ident)!!

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(personident) - bruker finnes ikke - returnerer null`() {
		repository.get("FNR") shouldBe null
	}

	@Test
	fun `getByPersonId(uuid) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testRepository.insertNavBruker(bruker)

		val faktiskBruker = repository.getByPersonId(bruker.person.id)!!

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `getByPersonId(uuid) - bruker finnes ikke - returnerer null`() {
		repository.getByPersonId(UUID.randomUUID()) shouldBe null
	}


	@Test
	fun `getAllNavBrukere - brukere finnes - returnerer brukere`() {
		val bruker1 = TestData.lagNavBruker()
		val bruker2 = TestData.lagNavBruker()
		val bruker3 = TestData.lagNavBruker()

		testRepository.insertNavBruker(bruker1)
		testRepository.insertNavBruker(bruker2)
		testRepository.insertNavBruker(bruker3)

		val brukere = repository.getAllNavBrukere(0, 2)

		brukere.size shouldBe 2

		sammenlign(brukere[0], bruker1)
		sammenlign(brukere[1], bruker2)

	}

	@Test
	fun `upsert - bruker finnes ikke - inserter ny bruker`() {
		val bruker = TestData.lagNavBruker()

		testRepository.insertPerson(bruker.person)
		testRepository.insertNavAnsatt(bruker.navVeileder!!)
		testRepository.insertNavEnhet(bruker.navEnhet!!)

		repository.upsert(NavBrukerUpsert(
			id = bruker.id,
			personId = bruker.person.id,
			navVeilederId = bruker.navVeileder?.id,
			navEnhetId = bruker.navEnhet?.id,
			telefon = bruker.telefon,
			epost = bruker.epost,
			erSkjermet = bruker.erSkjermet,
			adresse = bruker.adresse,
			adressebeskyttelse = bruker.adressebeskyttelse,
			oppfolgingsperioder = bruker.oppfolgingsperioder,
			innsatsgruppe = bruker.innsatsgruppe
		))

		val faktiskBruker = repository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `upsert - bruker finnes ikke, har adressebeskyttelse - inserter ny bruker`() {
		val bruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.FORTROLIG, adresse = null)

		testRepository.insertPerson(bruker.person)
		testRepository.insertNavAnsatt(bruker.navVeileder!!)
		testRepository.insertNavEnhet(bruker.navEnhet!!)

		repository.upsert(NavBrukerUpsert(
			id = bruker.id,
			personId = bruker.person.id,
			navVeilederId = bruker.navVeileder?.id,
			navEnhetId = bruker.navEnhet?.id,
			telefon = bruker.telefon,
			epost = bruker.epost,
			erSkjermet = bruker.erSkjermet,
			adresse = bruker.adresse,
			adressebeskyttelse = bruker.adressebeskyttelse,
			oppfolgingsperioder = bruker.oppfolgingsperioder,
			innsatsgruppe = bruker.innsatsgruppe
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
			erSkjermet = true,
			adresse = Adresse(
				bostedsadresse = null,
				oppholdsadresse = null,
				kontaktadresse = Kontaktadresse(
					coAdressenavn = null,
					vegadresse = Vegadresse(
						husnummer = "1",
						husbokstav = null,
						adressenavn = "Gate",
						tilleggsnavn = null,
						postnummer = "1234",
						poststed = "MOSS"
					),
					postboksadresse = null
				)
			),
			adressebeskyttelse = null,
			oppfolgingsperioder = bruker.oppfolgingsperioder,
			innsatsgruppe = InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS
		)

		repository.upsert(upsert)

		val faktiskBruker = repository.get(bruker.id)

		faktiskBruker.navVeileder shouldBe null
		faktiskBruker.navEnhet shouldBe null
		faktiskBruker.erSkjermet shouldBe true

		faktiskBruker.telefon shouldBe upsert.telefon
		faktiskBruker.epost shouldBe upsert.epost

		faktiskBruker.adresse?.kontaktadresse?.vegadresse?.husnummer shouldBe "1"
		faktiskBruker.adresse?.kontaktadresse?.vegadresse?.adressenavn shouldBe "Gate"
		faktiskBruker.adresse?.kontaktadresse?.vegadresse?.postnummer shouldBe "1234"
		faktiskBruker.adresse?.kontaktadresse?.vegadresse?.poststed shouldBe "MOSS"

		faktiskBruker.oppfolgingsperioder shouldBe upsert.oppfolgingsperioder
		faktiskBruker.innsatsgruppe shouldBe InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS

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

		repository.finnBrukerId(bruker.person.personident) shouldBe bruker.id
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

		repository.get(bruker.person.personident) shouldBe null
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
		faktiskBruker.adressebeskyttelse shouldBe bruker.adressebeskyttelse
		faktiskBruker.oppfolgingsperioder shouldBe bruker.oppfolgingsperioder
		faktiskBruker.innsatsgruppe shouldBe bruker.innsatsgruppe
	}

}
