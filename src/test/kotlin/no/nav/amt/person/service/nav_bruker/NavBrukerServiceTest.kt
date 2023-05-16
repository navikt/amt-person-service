package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.mockExecuteWithoutResult
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.support.TransactionTemplate

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
	lateinit var kafkaProducerService: KafkaProducerService
	lateinit var transactionTemplate: TransactionTemplate

	@BeforeEach
	fun setup() {
		repository = mockk(relaxUnitFun = true)
		personService = mockk(relaxUnitFun = true)
		navAnsattService = mockk()
		navEnhetService = mockk()
		krrProxyClient = mockk()
		poaoTilgangClient = mockk()
		pdlClient = mockk()
		rolleService = mockk(relaxUnitFun = true)
		kafkaProducerService = mockk(relaxUnitFun = true)
		transactionTemplate = mockk()

		service = NavBrukerService(
			repository = repository,
			personService = personService,
			navAnsattService = navAnsattService,
			navEnhetService = navEnhetService,
			rolleService = rolleService,
			krrProxyClient = krrProxyClient,
			poaoTilgangClient = poaoTilgangClient,
			pdlClient = pdlClient,
			kafkaProducerService = kafkaProducerService,
			transactionTemplate = transactionTemplate,
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
		every { personService.hentEllerOpprettPerson(person.personIdent, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personIdent) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personIdent) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personIdent) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personIdent) } returns ApiResult(result = erSkjermet, throwable = null)
		every { rolleService.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		mockExecuteWithoutResult(transactionTemplate)

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

		assertThrows<IllegalStateException> {
			service.hentEllerOpprettNavBruker(person.personIdent)
		}
	}

	@Test
	fun `oppdaterKontaktInformasjon - ingen brukere finnes - oppdaterer ikke`() {
		val personer = listOf(TestData.lagPerson().toModel(), TestData.lagPerson().toModel())

		every { repository.get(any<String>()) } returns null

		service.oppdaterKontaktinformasjon(personer)

		verify(exactly = 0) { pdlClient.hentTelefon(any()) }
		verify(exactly = 0) { krrProxyClient.hentKontaktinformasjon(any()) }
		verify(exactly = 0) { repository.upsert(any()) }
	}

	@Test
	fun `oppdaterKontaktInformasjon - telefon er registrert i krr - oppdaterer bruker med telefon fra krr`() {
		val bruker = TestData.lagNavBruker()
		val kontakinformasjon = Kontaktinformasjon(
				"ny epost",
				"krr-telefon",
			)

		every { repository.finnBrukerId(bruker.person.personIdent) } returns bruker.id
		every { repository.get(bruker.person.personIdent) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) } returns Result.success(kontakinformasjon)
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(listOf(bruker.person.toModel()))

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) }
		verify(exactly = 1) { repository.upsert(
				bruker.copy(telefon = kontakinformasjon.telefonnummer, epost = kontakinformasjon.epost)
					.toModel()
					.toUpsert()
			)
		}
	}
	@Test
	fun `oppdaterKontaktInformasjon - telefon er ikke registrert i krr - oppdaterer bruker med telefon fra pdl`() {
		val bruker = TestData.lagNavBruker()
		val kontakinformasjon = Kontaktinformasjon(
			"ny epost",
			null,
		)
		val pdlTelefon = "pdl-telefon"

		every { repository.finnBrukerId(bruker.person.personIdent) } returns bruker.id
		every { pdlClient.hentTelefon(bruker.person.personIdent) } returns pdlTelefon
		every { repository.get(bruker.person.personIdent) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) } returns Result.success(kontakinformasjon)
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(listOf(bruker.person.toModel()))

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personIdent) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) }
		verify(exactly = 1) { repository.upsert(
			bruker.copy(telefon = pdlTelefon, epost = kontakinformasjon.epost)
				.toModel()
				.toUpsert()
		) }
	}

	@Test
	fun `oppdaterKontaktInformasjon - krr feiler - oppdaterer ikke`() {
		val bruker = TestData.lagNavBruker()

		every { repository.finnBrukerId(bruker.person.personIdent) } returns bruker.id
		every { repository.get(bruker.person.personIdent) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) } returns Result.failure(RuntimeException())

		service.oppdaterKontaktinformasjon(listOf(bruker.person.toModel()))

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personIdent) }
		verify(exactly = 0) { repository.upsert(any()) }
	}

	@Test
	fun `slettBruker - bruker har kun NAV_BRUKER-rolle - sletter bruker og person`() {
		val bruker = TestData.lagNavBruker()

		every { rolleService.harRolle(bruker.person.id, Rolle.ARRANGOR_ANSATT) } returns false
		mockExecuteWithoutResult(transactionTemplate)

		service.slettBruker(bruker.toModel())

		verify { repository.delete(bruker.id) }
		verify { rolleService.fjernRolle(bruker.person.id, Rolle.NAV_BRUKER) }
		verify { personService.slettPerson(bruker.person.toModel()) }
		verify { kafkaProducerService.publiserSlettNavBruker(bruker.id) }
	}

	@Test
	fun `slettBruker - bruker har andre roller - sletter bruker og men ikke person`() {
		val bruker = TestData.lagNavBruker()

		every { rolleService.harRolle(bruker.person.id, Rolle.ARRANGOR_ANSATT) } returns true
		mockExecuteWithoutResult(transactionTemplate)

		service.slettBruker(bruker.toModel())

		verify { repository.delete(bruker.id) }
		verify { rolleService.fjernRolle(bruker.person.id, Rolle.NAV_BRUKER) }
		verify(exactly = 0) { personService.slettPerson(bruker.person.toModel()) }
		verify { kafkaProducerService.publiserSlettNavBruker(bruker.id) }
	}
}
