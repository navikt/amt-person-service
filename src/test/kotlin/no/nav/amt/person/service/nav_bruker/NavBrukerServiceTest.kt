package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KontaktinformasjonForPersoner
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
import java.time.LocalDateTime
import java.util.UUID

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
		val navBruker = TestData.lagNavBruker()
		val person = navBruker.person
		val personOpplysninger = TestData.lagPdlPerson(person = person)

		val veileder =  navBruker.navVeileder!!
		val navEnhet = navBruker.navEnhet!!
		val kontaktinformasjon = Kontaktinformasjon(navBruker.epost, navBruker.telefon)
		val erSkjermet = navBruker.erSkjermet

		every { repository.get(person.personident) } returns null
		every { pdlClient.hentPerson(person.personident) } returns personOpplysninger
		every { personService.hentEllerOpprettPerson(person.personident, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personident) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personident) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personident) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personident) } returns ApiResult(result = erSkjermet, throwable = null)
		every { rolleService.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		every { repository.getByPersonId(person.id) } returns navBruker
		mockExecuteWithoutResult(transactionTemplate)

		val faktiskBruker = service.hentEllerOpprettNavBruker(person.personident)

		faktiskBruker.person.id shouldBe person.id
		faktiskBruker.navVeileder?.id shouldBe veileder.id
		faktiskBruker.navEnhet?.id shouldBe navEnhet.id
		faktiskBruker.telefon shouldBe kontaktinformasjon.telefonnummer
		faktiskBruker.epost shouldBe kontaktinformasjon.epost
		faktiskBruker.erSkjermet shouldBe erSkjermet
		faktiskBruker.adressebeskyttelse shouldBe null
	}

	@Test
	fun `hentEllerOpprettNavBruker - bruker er adressebeskyttet - oppretter bruker`() {
		val navBruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG)
		val person = navBruker.person
		val personOpplysninger = TestData.lagPdlPerson(person, adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG)
		val veileder =  navBruker.navVeileder!!
		val navEnhet = navBruker.navEnhet!!
		val kontaktinformasjon = Kontaktinformasjon(navBruker.epost, navBruker.telefon)
		val erSkjermet = navBruker.erSkjermet

		every { repository.get(person.personident) } returns null
		every { pdlClient.hentPerson(person.personident) } returns personOpplysninger
		every { personService.hentEllerOpprettPerson(person.personident, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personident) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personident) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personident) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personident) } returns ApiResult(result = erSkjermet, throwable = null)
		every { rolleService.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		every { repository.getByPersonId(person.id) } returns navBruker
		mockExecuteWithoutResult(transactionTemplate)

		val faktiskBruker = service.hentEllerOpprettNavBruker(person.personident)

		faktiskBruker.person.id shouldBe person.id
		faktiskBruker.navVeileder?.id shouldBe veileder.id
		faktiskBruker.navEnhet?.id shouldBe navEnhet.id
		faktiskBruker.telefon shouldBe kontaktinformasjon.telefonnummer
		faktiskBruker.epost shouldBe kontaktinformasjon.epost
		faktiskBruker.erSkjermet shouldBe erSkjermet
		faktiskBruker.adresse shouldBe null
		faktiskBruker.adressebeskyttelse shouldBe Adressebeskyttelse.STRENGT_FORTROLIG
	}

	@Test
	fun `syncKontaktinfoBulk - telefon er registrert i krr - oppdaterer bruker med telefon fra krr`() {
		val bruker = TestData.lagNavBruker()
		val kontaktinfo = Kontaktinformasjon(
			"ny epost",
			"krr-telefon",
		)
		val kontakinfoForPersoner = KontaktinformasjonForPersoner(mapOf(bruker.person.personident to kontaktinfo))

		every { repository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns Result.success(kontakinfoForPersoner)

		mockExecuteWithoutResult(transactionTemplate)

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		val expectedData = bruker.copy(
			telefon = kontaktinfo.telefonnummer,
			epost = kontaktinfo.epost,
			sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
		)
			.toModel()
			.toUpsert()


		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 1) { repository.upsert( match {
			expectedData.id == it.id &&
				expectedData.personId == it.personId &&
				expectedData.navEnhetId == it.navEnhetId &&
				expectedData.navVeilederId == it.navVeilederId &&
				expectedData.telefon == it.telefon &&
				expectedData.epost == it.epost &&
				expectedData.erSkjermet == it.erSkjermet &&
				expectedData.adresse == it.adresse &&
				expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)

		})}
	}

	@Test
	fun `syncKontaktinfoBulk - telefon er ikke registrert i krr - oppdaterer bruker med telefon fra pdl`() {
		val bruker = TestData.lagNavBruker()
		val krrKontaktinfo = Kontaktinformasjon(
			"ny epost",
			null,
		)
		val kontakinfoForPersoner = KontaktinformasjonForPersoner(mapOf(bruker.person.personident to krrKontaktinfo))

		val pdlTelefon = "pdl-telefon"

		every { repository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { pdlClient.hentTelefon(bruker.person.personident) } returns pdlTelefon
		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns Result.success(kontakinfoForPersoner)

		mockExecuteWithoutResult(transactionTemplate)

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		val expectedData = bruker.copy(
			telefon = pdlTelefon,
			epost = krrKontaktinfo.epost,
			sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
		)
			.toModel()
			.toUpsert()

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personident) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 1) { repository.upsert(match {
			expectedData.id == it.id &&
				expectedData.personId == it.personId &&
				expectedData.navEnhetId == it.navEnhetId &&
				expectedData.navVeilederId == it.navVeilederId &&
				expectedData.telefon == it.telefon &&
				expectedData.epost == it.epost &&
				expectedData.erSkjermet == it.erSkjermet &&
				expectedData.adresse == it.adresse &&
				expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)

		}) }
	}

	@Test
	fun `syncKontaktinfoBulk - krr feiler - oppdaterer ikke`() {
		val bruker = TestData.lagNavBruker()

		every { repository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns Result.failure(RuntimeException())

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 0) { repository.upsert(any()) }
	}

	@Test
	fun `oppdaterKontaktinformasjon - bruker har ny kontaktinfo - oppdaterer bruker`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))
		val kontaktinfo = Kontaktinformasjon(
			"ny epost",
			"krr-telefon",
		)

		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns Result.success(kontaktinfo)

		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(bruker.toModel())

		val expectedData = bruker.copy(
			telefon = kontaktinfo.telefonnummer,
			epost = kontaktinfo.epost,
			sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
		)
			.toModel()
			.toUpsert()


		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
		verify(exactly = 1) { repository.upsert( match {
			expectedData.id == it.id &&
				expectedData.personId == it.personId &&
				expectedData.navEnhetId == it.navEnhetId &&
				expectedData.navVeilederId == it.navVeilederId &&
				expectedData.telefon == it.telefon &&
				expectedData.epost == it.epost &&
				expectedData.erSkjermet == it.erSkjermet &&
				expectedData.adresse == it.adresse &&
				expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)

		})}
	}

	@Test
	fun `oppdaterKontaktinformasjon - telefon er ikke registrert i krr - oppdaterer bruker med telefon fra pdl`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))
		val krrKontaktinfo = Kontaktinformasjon(
			"ny epost",
			null,
		)

		val pdlTelefon = "pdl-telefon"

		every { pdlClient.hentTelefon(bruker.person.personident) } returns pdlTelefon
		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns Result.success(krrKontaktinfo)

		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(bruker.toModel())

		val expectedData = bruker.copy(
			telefon = pdlTelefon,
			epost = krrKontaktinfo.epost,
			sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
		)
			.toModel()
			.toUpsert()

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personident) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
		verify(exactly = 1) { repository.upsert(match {
			expectedData.id == it.id &&
				expectedData.personId == it.personId &&
				expectedData.navEnhetId == it.navEnhetId &&
				expectedData.navVeilederId == it.navVeilederId &&
				expectedData.telefon == it.telefon &&
				expectedData.epost == it.epost &&
				expectedData.erSkjermet == it.erSkjermet &&
				expectedData.adresse == it.adresse &&
				expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)

		}) }
	}

	@Test
	fun `oppdaterKontaktinformasjon - krr feiler - oppdaterer ikke`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))

		every { repository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns Result.failure(RuntimeException())

		service.oppdaterKontaktinformasjon(bruker.toModel())

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
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
		verify { kafkaProducerService.publiserSlettNavBruker(bruker.person.id) }
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
		verify { kafkaProducerService.publiserSlettNavBruker(bruker.person.id) }
	}
}
