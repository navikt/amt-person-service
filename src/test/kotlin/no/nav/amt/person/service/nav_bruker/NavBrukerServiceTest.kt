package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Rolle
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NavBrukerServiceTest {
	lateinit var service: NavBrukerService
	lateinit var repository: NavBrukerRepository
	lateinit var personService: PersonService
	lateinit var navAnsattService: NavAnsattService
	lateinit var navEnhetService: NavEnhetService
	lateinit var rolleService: RolleService
	lateinit var krrProxyClient: KrrProxyClient
	lateinit var poaoTilgangClient: PoaoTilgangClient
	lateinit var pdlClient: PdlClient

	@BeforeEach
	fun setup() {
		repository = mockk(relaxUnitFun = true)
		personService = mockk()
		navAnsattService = mockk()
		navEnhetService = mockk()
		krrProxyClient = mockk()
		poaoTilgangClient = mockk()
		pdlClient = mockk()
		rolleService = mockk(relaxUnitFun = true)

		service = NavBrukerService(
			repository = repository,
			personService = personService,
			navAnsattService = navAnsattService,
			navEnhetService = navEnhetService,
			rolleService = rolleService,
			krrProxyClient = krrProxyClient,
			poaoTilgangClient = poaoTilgangClient,
			pdlClient = pdlClient,
		)
	}

	@Test
	fun `hentEllerOpprettNavBruker - bruker finnes ikke - oppretter og returnerer ny bruker`() {
		val person = TestData.lagPerson()
		val personOpplysninger = TestData.lagPdlPerson(person = person)

		val veileder =  TestData.lagNavAnsatt()
		val navEnhet = TestData.lagNavEnhet()
		val kontaktinformasjon = Kontaktinformasjon("navbruker@gmail.com", "99900111")
		val erSkjermet = false

		every { repository.get(person.personIdent) } returns null
		every { pdlClient.hentPerson(person.personIdent) } returns personOpplysninger
		every { personService.erAdressebeskyttet(any()) } returns false
		every { personService.hentEllerOpprettPerson(person.personIdent, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personIdent) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personIdent) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personIdent) } returns kontaktinformasjon
		every { poaoTilgangClient.erSkjermetPerson(person.personIdent) } returns ApiResult(result = erSkjermet, throwable = null)
		every { rolleService.harRolle(person.id, Rolle.NAV_BRUKER) } returns false

		val faktiskBruker = service.hentEllerOpprettNavBruker(person.personIdent)

		faktiskBruker.person.id shouldBe person.id
		faktiskBruker.navVeileder?.id shouldBe veileder.id
		faktiskBruker.navEnhet?.id shouldBe navEnhet.id
		faktiskBruker.telefon shouldBe kontaktinformasjon.telefonnummer
		faktiskBruker.epost shouldBe kontaktinformasjon.epost
		faktiskBruker.erSkjermet shouldBe erSkjermet
	}

	@Test
	fun `hentEllerOpprettNavBruker - bruker er adressebeskyttet - oppretter ikke bruker`() {
		val person = TestData.lagPerson()
		val personOpplysninger = TestData.lagPdlPerson(person, adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG)

		every { repository.get(person.personIdent) } returns null
		every { pdlClient.hentPerson(person.personIdent) } returns personOpplysninger
		every { personService.erAdressebeskyttet(personOpplysninger.adressebeskyttelseGradering) } returns true

		assertThrows<IllegalStateException> {
			service.hentEllerOpprettNavBruker(person.personIdent)
		}
	}

	@Test
	fun `oppdaterKontaktInformasjon - ingen brukere finnes - oppdaterer ikke`() {
		val personer = listOf(TestData.lagPerson().toModel(), TestData.lagPerson().toModel())

		every { repository.finnBrukerId(any()) } returns null

		service.oppdaterKontaktinformasjon(personer)

		verify(exactly = 0) { pdlClient.hentTelefon(any()) }
		verify(exactly = 0) { krrProxyClient.hentKontaktinformasjon(any()) }
		verify(exactly = 0) { repository.oppdaterKontaktinformasjon(any(), any(), any()) }
	}

	@Test
	fun `oppdaterKontaktInformasjon - telefon er registrert i krr og pdl - oppdaterer bruker med telefon fra krr`() {
		val bruker = TestData.lagNavBruker()
		val kontakinformasjon = Kontaktinformasjon(
				"ny epost",
				"krr-telefon",
			)

		every { repository.finnBrukerId(bruker.person.personIdent) } returns bruker.id
		every { pdlClient.hentTelefon(bruker.person.personIdent) } returns "pdl-telefon"
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) } returns kontakinformasjon

		service.oppdaterKontaktinformasjon(listOf(bruker.person.toModel()))

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personIdent) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) }
		verify(exactly = 1) { repository.oppdaterKontaktinformasjon(bruker.id, kontakinformasjon.telefonnummer, kontakinformasjon.epost) }
	}

}
