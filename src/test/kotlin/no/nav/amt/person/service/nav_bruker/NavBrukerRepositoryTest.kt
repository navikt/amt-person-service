package no.nav.amt.person.service.nav_bruker

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.RepositoryTestBase
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Kontaktadresse
import no.nav.amt.person.service.person.model.Vegadresse
import no.nav.amt.person.service.utils.shouldBeCloseTo
import no.nav.amt.person.service.utils.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(classes = [NavBrukerRepository::class])
class NavBrukerRepositoryTest(
	private val brukerRepository: NavBrukerRepository,
) : RepositoryTestBase() {

	@Test
	fun `get(uuid) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val faktiskBruker = brukerRepository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `get(uuid) - bruker finnes ikke - kaster NoSuchElementException`() {
		assertThrows<NoSuchElementException> {
			brukerRepository.get(UUID.randomUUID())
		}
	}

	@Test
	fun `get(personident) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val faktiskBruker = brukerRepository.get(bruker.person.personident)
		sammenlign(faktiskBruker.shouldNotBeNull(), bruker)
	}

	@Test
	fun `get(personident) - søk med historisk ident, bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val historiskIdent = TestData.lagPersonident(personId = bruker.person.id, historisk = true)
		testDataRepository.insertPersonidenter(listOf(historiskIdent))

		val faktiskBruker = brukerRepository.get(historiskIdent.ident)
		sammenlign(faktiskBruker.shouldNotBeNull(), bruker)
	}

	@Test
	fun `get(personident) - bruker finnes ikke - returnerer null`() {
		brukerRepository.get("FNR") shouldBe null
	}

	@Test
	fun `getByPersonId(uuid) - bruker finnes - returnerer bruker`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		val faktiskBruker = brukerRepository.getByPersonId(bruker.person.id)

		sammenlign(faktiskBruker.shouldNotBeNull(), bruker)
	}

	@Test
	fun `getByPersonId(uuid) - bruker finnes ikke - returnerer null`() {
		brukerRepository.getByPersonId(UUID.randomUUID()) shouldBe null
	}

	@Test
	fun `getAllNavBrukere - brukere finnes - returnerer brukere`() {
		val bruker1 = TestData.lagNavBruker()
		val bruker2 = TestData.lagNavBruker()
		val bruker3 = TestData.lagNavBruker()

		testDataRepository.insertNavBruker(bruker1)
		testDataRepository.insertNavBruker(bruker2)
		testDataRepository.insertNavBruker(bruker3)

		val brukere = brukerRepository.getAllNavBrukere(0, 2)

		brukere.size shouldBe 2

		sammenlign(brukere[0], bruker1)
		sammenlign(brukere[1], bruker2)
	}

	@Test
	fun `upsert - bruker finnes ikke - inserter ny bruker`() {
		val bruker = TestData.lagNavBruker()

		testDataRepository.insertPerson(bruker.person)
		testDataRepository.insertNavAnsatt(bruker.navVeileder!!)
		testDataRepository.insertNavEnhet(bruker.navEnhet!!)

		brukerRepository.upsert(
			NavBrukerUpsert(
				id = bruker.id,
				personId = bruker.person.id,
				navVeilederId = bruker.navVeileder.id,
				navEnhetId = bruker.navEnhet.id,
				telefon = bruker.telefon,
				epost = bruker.epost,
				erSkjermet = bruker.erSkjermet,
				adresse = bruker.adresse,
				adressebeskyttelse = bruker.adressebeskyttelse,
				oppfolgingsperioder = bruker.oppfolgingsperioder,
				innsatsgruppe = bruker.innsatsgruppe
			)
		)

		val faktiskBruker = brukerRepository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `upsert - bruker finnes ikke, har adressebeskyttelse - inserter ny bruker`() {
		val bruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.FORTROLIG, adresse = null)

		testDataRepository.insertPerson(bruker.person)
		testDataRepository.insertNavAnsatt(bruker.navVeileder!!)
		testDataRepository.insertNavEnhet(bruker.navEnhet!!)

		brukerRepository.upsert(
			NavBrukerUpsert(
				id = bruker.id,
				personId = bruker.person.id,
				navVeilederId = bruker.navVeileder.id,
				navEnhetId = bruker.navEnhet.id,
				telefon = bruker.telefon,
				epost = bruker.epost,
				erSkjermet = bruker.erSkjermet,
				adresse = bruker.adresse,
				adressebeskyttelse = bruker.adressebeskyttelse,
				oppfolgingsperioder = bruker.oppfolgingsperioder,
				innsatsgruppe = bruker.innsatsgruppe
			)
		)

		val faktiskBruker = brukerRepository.get(bruker.id)

		sammenlign(faktiskBruker, bruker)
	}

	@Test
	fun `upsert - bruker finnes - oppdaterer bruker`() {
		val bruker = TestData.lagNavBruker(
			createdAt = LocalDateTime.now().minusMonths(6),
			modifiedAt = LocalDateTime.now().minusMonths(6),
			erSkjermet = false,
		)
		testDataRepository.insertNavBruker(bruker)

		brukerRepository.get(bruker.id)

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

		brukerRepository.upsert(upsert)

		val faktiskBruker = brukerRepository.get(bruker.id)

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
		brukerRepository.finnBrukerId("en ident") shouldBe null
	}

	@Test
	fun `finnBrukerId - bruker finnes - returnerer id`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		brukerRepository.finnBrukerId(bruker.person.personident) shouldBe bruker.id
	}

	@Test
	fun `finnBrukerId - søk med historisk ident, bruker finnes - returnerer id`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)
		val historiskIdent = TestData.lagPersonident(personId = bruker.person.id, historisk = true)
		testDataRepository.insertPersonidenter(listOf(historiskIdent))

		brukerRepository.finnBrukerId(historiskIdent.ident) shouldBe bruker.id
	}

	@Test
	fun `delete- bruker finnes - sletter bruker`() {
		val bruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(bruker)

		brukerRepository.delete(bruker.id)

		brukerRepository.get(bruker.person.personident) shouldBe null
	}

	private fun sammenlign(
		faktiskBruker: NavBrukerDbo,
		bruker: NavBrukerDbo,
	) {
		assertSoftly(faktiskBruker) {
			id shouldBe bruker.id
			person.id shouldBe bruker.person.id
			navVeileder?.id shouldBe bruker.navVeileder?.id
			navEnhet?.id shouldBe bruker.navEnhet?.id
			telefon shouldBe bruker.telefon
			epost shouldBe bruker.epost
			erSkjermet shouldBe bruker.erSkjermet
			createdAt shouldBeCloseTo bruker.createdAt
			modifiedAt shouldBeCloseTo bruker.modifiedAt
			adressebeskyttelse shouldBe bruker.adressebeskyttelse
			oppfolgingsperioder shouldBe bruker.oppfolgingsperioder
			innsatsgruppe shouldBe bruker.innsatsgruppe
		}
	}
}

