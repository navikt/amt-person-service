package no.nav.amt.person.service.navbruker

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.navansatt.NavAnsattService
import no.nav.amt.person.service.navenhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.RolleRepository
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.mockExecuteWithoutResult
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

class NavBrukerServiceTest {
	private val brukerRepository: NavBrukerRepository = mockk(relaxUnitFun = true)
	private val personService: PersonService = mockk(relaxUnitFun = true)
	private val navAnsattService: NavAnsattService = mockk()
	private val navEnhetService: NavEnhetService = mockk()
	private val rolleRepository: RolleRepository = mockk(relaxUnitFun = true)
	private val krrProxyClient: KrrProxyClient = mockk()
	private val poaoTilgangClient: PoaoTilgangClient = mockk()
	private val pdlClient: PdlClient = mockk()
	private val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
	private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient = mockk()
	private val kafkaProducerService: KafkaProducerService = mockk(relaxUnitFun = true)
	private val transactionTemplate: TransactionTemplate = mockk()

	private val service =
		NavBrukerService(
			navBrukerRepository = brukerRepository,
			rolleRepository = rolleRepository,
			personService = personService,
			navAnsattService = navAnsattService,
			navEnhetService = navEnhetService,
			krrProxyClient = krrProxyClient,
			poaoTilgangClient = poaoTilgangClient,
			pdlClient = pdlClient,
			veilarboppfolgingClient = veilarboppfolgingClient,
			veilarbvedtaksstotteClient = veilarbvedtaksstotteClient,
			kafkaProducerService = kafkaProducerService,
			transactionTemplate = transactionTemplate,
		)

	@BeforeEach
	fun setup() = clearAllMocks()

	@Test
	fun `hentEllerOpprettNavBruker - bruker finnes ikke - oppretter og returnerer ny bruker`() {
		val navBruker = TestData.lagNavBruker()
		val person = navBruker.person
		val personOpplysninger = TestData.lagPdlPerson(person = person)

		val veileder = navBruker.navVeileder.shouldNotBeNull()
		val navEnhet = navBruker.navEnhet.shouldNotBeNull()
		val kontaktinformasjon = Kontaktinformasjon(navBruker.epost, navBruker.telefon)
		val erSkjermet = navBruker.erSkjermet

		every { brukerRepository.get(person.personident) } returns null
		every { pdlClient.hentPerson(person.personident) } returns personOpplysninger
		every { veilarboppfolgingClient.hentOppfolgingperioder(person.personident) } returns navBruker.oppfolgingsperioder
		every { veilarbvedtaksstotteClient.hentInnsatsgruppe(person.personident) } returns navBruker.innsatsgruppe
		every { personService.hentEllerOpprettPerson(person.personident, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personident) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personident) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personident) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personident) } returns
			ApiResult(
				result = erSkjermet,
				throwable = null,
			)
		every { rolleRepository.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		every { brukerRepository.getByPersonId(person.id) } returns navBruker
		mockExecuteWithoutResult(transactionTemplate)

		val faktiskBruker = service.hentEllerOpprettNavBruker(person.personident)

		faktiskBruker.person.id shouldBe person.id
		faktiskBruker.navVeileder?.id shouldBe veileder.id
		faktiskBruker.navEnhet?.id shouldBe navEnhet.id
		faktiskBruker.telefon shouldBe kontaktinformasjon.telefonnummer
		faktiskBruker.epost shouldBe kontaktinformasjon.epost
		faktiskBruker.erSkjermet shouldBe erSkjermet
		faktiskBruker.adressebeskyttelse shouldBe null
		faktiskBruker.oppfolgingsperioder shouldBe navBruker.oppfolgingsperioder
		faktiskBruker.innsatsgruppe shouldBe navBruker.innsatsgruppe
	}

	@Test
	fun `hentEllerOpprettNavBruker - bruker er adressebeskyttet - oppretter bruker`() {
		val navBruker = TestData.lagNavBruker(adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG)
		val person = navBruker.person
		val personOpplysninger =
			TestData.lagPdlPerson(person, adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG)
		val veileder = navBruker.navVeileder.shouldNotBeNull()
		val navEnhet = navBruker.navEnhet.shouldNotBeNull()
		val kontaktinformasjon = Kontaktinformasjon(navBruker.epost, navBruker.telefon)
		val erSkjermet = navBruker.erSkjermet

		every { brukerRepository.get(person.personident) } returns null
		every { pdlClient.hentPerson(person.personident) } returns personOpplysninger
		every { veilarboppfolgingClient.hentOppfolgingperioder(person.personident) } returns navBruker.oppfolgingsperioder
		every { veilarbvedtaksstotteClient.hentInnsatsgruppe(person.personident) } returns navBruker.innsatsgruppe
		every { personService.hentEllerOpprettPerson(person.personident, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personident) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personident) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personident) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personident) } returns
			ApiResult(
				result = erSkjermet,
				throwable = null,
			)
		every { rolleRepository.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		every { brukerRepository.getByPersonId(person.id) } returns navBruker
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
	fun `hentEllerOpprettNavBruker - ikke aktiv oppfolgingsperiode - innsatsgruppe er null`() {
		val navBruker =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						TestData.lagOppfolgingsperiode(
							startdato = LocalDateTime.now().minusYears(1),
							sluttdato = LocalDateTime.now().minusDays(29),
						),
					),
			)
		val person = navBruker.person
		val personOpplysninger = TestData.lagPdlPerson(person = person)

		val veileder = navBruker.navVeileder.shouldNotBeNull()
		val navEnhet = navBruker.navEnhet.shouldNotBeNull()
		val kontaktinformasjon = Kontaktinformasjon(navBruker.epost, navBruker.telefon)
		val erSkjermet = navBruker.erSkjermet

		every { brukerRepository.get(person.personident) } returns null
		every { pdlClient.hentPerson(person.personident) } returns personOpplysninger
		every { veilarboppfolgingClient.hentOppfolgingperioder(person.personident) } returns navBruker.oppfolgingsperioder
		every { veilarbvedtaksstotteClient.hentInnsatsgruppe(person.personident) } returns navBruker.innsatsgruppe
		every { personService.hentEllerOpprettPerson(person.personident, personOpplysninger) } returns person.toModel()
		every { navAnsattService.hentBrukersVeileder(person.personident) } returns veileder.toModel()
		every { navEnhetService.hentNavEnhetForBruker(person.personident) } returns navEnhet.toModel()
		every { krrProxyClient.hentKontaktinformasjon(person.personident) } returns Result.success(kontaktinformasjon)
		every { poaoTilgangClient.erSkjermetPerson(person.personident) } returns
			ApiResult(
				result = erSkjermet,
				throwable = null,
			)
		every { rolleRepository.harRolle(person.id, Rolle.NAV_BRUKER) } returns false
		every { brukerRepository.getByPersonId(person.id) } returns navBruker.copy(innsatsgruppe = null)
		mockExecuteWithoutResult(transactionTemplate)

		val faktiskBruker = service.hentEllerOpprettNavBruker(person.personident)

		faktiskBruker.person.id shouldBe person.id
		faktiskBruker.oppfolgingsperioder shouldBe navBruker.oppfolgingsperioder
		faktiskBruker.innsatsgruppe shouldBe null
	}

	@Test
	fun `syncKontaktinfoBulk - telefon er registrert i krr - oppdaterer bruker med telefon fra krr`() {
		val bruker = TestData.lagNavBruker()
		val kontaktinfo =
			Kontaktinformasjon(
				"ny epost",
				"krr-telefon",
			)
		val kontakinfoForPersoner = mapOf(bruker.person.personident to kontaktinfo)

		every { brukerRepository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns
			Result.success(
				kontakinfoForPersoner,
			)

		mockExecuteWithoutResult(transactionTemplate)

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		val expectedData =
			bruker
				.copy(
					telefon = kontaktinfo.telefonnummer,
					epost = kontaktinfo.epost,
					sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
				).toModel()
				.toUpsert()

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					expectedData.id == it.id &&
						expectedData.personId == it.personId &&
						expectedData.navEnhetId == it.navEnhetId &&
						expectedData.navVeilederId == it.navVeilederId &&
						expectedData.telefon == it.telefon &&
						expectedData.epost == it.epost &&
						expectedData.erSkjermet == it.erSkjermet &&
						expectedData.adresse == it.adresse &&
						expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)
				},
			)
		}
	}

	@Test
	fun `syncKontaktinfoBulk - telefon er ikke registrert i krr - oppdaterer bruker med telefon fra pdl`() {
		val bruker = TestData.lagNavBruker()
		val krrKontaktinfo =
			Kontaktinformasjon(
				"ny epost",
				null,
			)
		val kontakinfoForPersoner = mapOf(bruker.person.personident to krrKontaktinfo)

		val pdlTelefon = "pdl-telefon"

		every { brukerRepository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { pdlClient.hentTelefon(bruker.person.personident) } returns pdlTelefon
		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns
			Result.success(
				kontakinfoForPersoner,
			)

		mockExecuteWithoutResult(transactionTemplate)

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		val expectedData =
			bruker
				.copy(
					telefon = pdlTelefon,
					epost = krrKontaktinfo.epost,
					sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
				).toModel()
				.toUpsert()

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personident) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					expectedData.id == it.id &&
						expectedData.personId == it.personId &&
						expectedData.navEnhetId == it.navEnhetId &&
						expectedData.navVeilederId == it.navVeilederId &&
						expectedData.telefon == it.telefon &&
						expectedData.epost == it.epost &&
						expectedData.erSkjermet == it.erSkjermet &&
						expectedData.adresse == it.adresse &&
						expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)
				},
			)
		}
	}

	@Test
	fun `syncKontaktinfoBulk - krr feiler - oppdaterer ikke`() {
		val bruker = TestData.lagNavBruker()

		every { brukerRepository.finnBrukerId(bruker.person.personident) } returns bruker.id
		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) } returns
			Result.failure(
				RuntimeException(),
			)

		service.syncKontaktinfoBulk(listOf(bruker.person.personident))

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(setOf(bruker.person.personident)) }
		verify(exactly = 0) { brukerRepository.upsert(any()) }
	}

	@Test
	fun `oppdaterKontaktinformasjon - bruker har ny kontaktinfo - oppdaterer bruker`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))
		val kontaktinfo =
			Kontaktinformasjon(
				"ny epost",
				"krr-telefon",
			)

		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns Result.success(kontaktinfo)

		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(bruker.toModel())

		val expectedData =
			bruker
				.copy(
					telefon = kontaktinfo.telefonnummer,
					epost = kontaktinfo.epost,
					sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
				).toModel()
				.toUpsert()

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					expectedData.id == it.id &&
						expectedData.personId == it.personId &&
						expectedData.navEnhetId == it.navEnhetId &&
						expectedData.navVeilederId == it.navVeilederId &&
						expectedData.telefon == it.telefon &&
						expectedData.epost == it.epost &&
						expectedData.erSkjermet == it.erSkjermet &&
						expectedData.adresse == it.adresse &&
						expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)
				},
			)
		}
	}

	@Test
	fun `oppdaterKontaktinformasjon - telefon er ikke registrert i krr - oppdaterer bruker med telefon fra pdl`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))
		val krrKontaktinfo =
			Kontaktinformasjon(
				"ny epost",
				null,
			)

		val pdlTelefon = "pdl-telefon"

		every { pdlClient.hentTelefon(bruker.person.personident) } returns pdlTelefon
		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns Result.success(krrKontaktinfo)

		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterKontaktinformasjon(bruker.toModel())

		val expectedData =
			bruker
				.copy(
					telefon = pdlTelefon,
					epost = krrKontaktinfo.epost,
					sisteKrrSync = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
				).toModel()
				.toUpsert()

		verify(exactly = 1) { pdlClient.hentTelefon(bruker.person.personident) }
		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					expectedData.id == it.id &&
						expectedData.personId == it.personId &&
						expectedData.navEnhetId == it.navEnhetId &&
						expectedData.navVeilederId == it.navVeilederId &&
						expectedData.telefon == it.telefon &&
						expectedData.epost == it.epost &&
						expectedData.erSkjermet == it.erSkjermet &&
						expectedData.adresse == it.adresse &&
						expectedData.sisteKrrSync == it.sisteKrrSync!!.truncatedTo(java.time.temporal.ChronoUnit.DAYS)
				},
			)
		}
	}

	@Test
	fun `oppdaterKontaktinformasjon - krr feiler - oppdaterer ikke`() {
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4))

		every { brukerRepository.get(bruker.person.personident) } returns bruker
		every { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) } returns
			Result.failure(
				RuntimeException(),
			)

		service.oppdaterKontaktinformasjon(bruker.toModel())

		verify(exactly = 1) { krrProxyClient.hentKontaktinformasjon(bruker.person.personident) }
		verify(exactly = 0) { brukerRepository.upsert(any()) }
	}

	@Nested
	inner class OppdaterOppfolgingsperiodeOgInnsatsgruppeTests {
		val brukerInTest =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						Oppfolgingsperiode(
							id = UUID.randomUUID(),
							startdato = LocalDateTime.now().minusYears(3),
							sluttdato = LocalDateTime.now().minusYears(1),
						),
					),
			)

		@BeforeEach
		fun setup() {
			mockExecuteWithoutResult(transactionTemplate)

			every { brukerRepository.get(brukerInTest.id) } returns brukerInTest
			every {
				veilarbvedtaksstotteClient.hentInnsatsgruppe(brukerInTest.person.personident)
			} returns InnsatsgruppeV1.STANDARD_INNSATS
		}

		@Test
		fun `oppdaterOppfolgingsperiode - har ingen oppfolgingsperioder - lagrer`() {
			val bruker = brukerInTest.copy(oppfolgingsperioder = emptyList())
			val oppfolgingsperiode = TestData.lagOppfolgingsperiode()

			every { brukerRepository.get(bruker.id) } returns bruker

			service.oppdaterOppfolgingsperiodeOgInnsatsgruppe(bruker.id, oppfolgingsperiode)

			verify(exactly = 1) {
				brukerRepository.upsert(
					match {
						it.oppfolgingsperioder == listOf(oppfolgingsperiode)
					},
				)
				veilarbvedtaksstotteClient.hentInnsatsgruppe(bruker.person.personident)
			}
		}

		@Test
		fun `har eldre oppfolgingsperiode - lagrer`() {
			val oppfolgingsperiode = TestData.lagOppfolgingsperiode()

			service.oppdaterOppfolgingsperiodeOgInnsatsgruppe(brukerInTest.id, oppfolgingsperiode)

			verify(exactly = 1) {
				brukerRepository.upsert(
					match {
						it.oppfolgingsperioder.size == 2 &&
							it.oppfolgingsperioder.find { oppfolgingsPeriode -> oppfolgingsPeriode.id == brukerInTest.oppfolgingsperioder.first().id } ==
							brukerInTest.oppfolgingsperioder.first() &&
							it.oppfolgingsperioder.find { oppfolgingsPeriode -> oppfolgingsPeriode.id == oppfolgingsperiode.id } == oppfolgingsperiode
					},
				)
				veilarbvedtaksstotteClient.hentInnsatsgruppe(brukerInTest.person.personident)
			}
		}

		@Test
		fun `har samme oppfolgingsperiode, annen sluttdato - oppdaterer`() {
			val oppfolgingsperiode = brukerInTest.oppfolgingsperioder.first().copy(sluttdato = null)

			service.oppdaterOppfolgingsperiodeOgInnsatsgruppe(brukerInTest.id, oppfolgingsperiode)

			verify(exactly = 1) {
				brukerRepository.upsert(
					match {
						it.oppfolgingsperioder.size == 1 &&
							it.oppfolgingsperioder.find { oppfolgingsPeriode -> oppfolgingsPeriode.id == brukerInTest.oppfolgingsperioder.first().id } ==
							oppfolgingsperiode
					},
				)
				veilarbvedtaksstotteClient.hentInnsatsgruppe(brukerInTest.person.personident)
			}
		}

		@Test
		fun `har samme oppfolgingsperiode, ingen endring - oppdaterer ikke`() {
			val oppfolgingsperiode = brukerInTest.oppfolgingsperioder.first()

			service.oppdaterOppfolgingsperiodeOgInnsatsgruppe(brukerInTest.id, oppfolgingsperiode)

			verify(exactly = 0) {
				brukerRepository.upsert(any())
				veilarbvedtaksstotteClient.hentInnsatsgruppe(any())
			}
		}
	}

	@Test
	fun `oppdaterInnsatsgruppe - har aktiv oppfolgingsperiode - lagrer`() {
		val bruker =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						Oppfolgingsperiode(
							id = UUID.randomUUID(),
							startdato = LocalDateTime.now().minusYears(3),
							sluttdato = null,
						),
					),
				innsatsgruppe = InnsatsgruppeV1.STANDARD_INNSATS,
			)
		every { brukerRepository.get(bruker.id) } returns bruker
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterInnsatsgruppe(bruker.id, InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS)

		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					it.innsatsgruppe == InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS
				},
			)
		}
	}

	@Test
	fun `oppdaterInnsatsgruppe - har aktiv oppfolgingsperiode, ingen endring - lagrer ikke`() {
		val bruker =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						Oppfolgingsperiode(
							id = UUID.randomUUID(),
							startdato = LocalDateTime.now().minusYears(3),
							sluttdato = null,
						),
					),
				innsatsgruppe = InnsatsgruppeV1.STANDARD_INNSATS,
			)
		every { brukerRepository.get(bruker.id) } returns bruker
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterInnsatsgruppe(bruker.id, InnsatsgruppeV1.STANDARD_INNSATS)

		verify(exactly = 0) { brukerRepository.upsert(any()) }
	}

	@Test
	fun `oppdaterInnsatsgruppe - har ikke aktiv oppfolgingsperiode - lagrer innsatsgruppe null`() {
		val bruker =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						Oppfolgingsperiode(
							id = UUID.randomUUID(),
							startdato = LocalDateTime.now().minusYears(3),
							sluttdato = LocalDateTime.now().minusMonths(2),
						),
					),
				innsatsgruppe = InnsatsgruppeV1.STANDARD_INNSATS,
			)
		every { brukerRepository.get(bruker.id) } returns bruker
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterInnsatsgruppe(bruker.id, InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS)

		verify(exactly = 1) {
			brukerRepository.upsert(
				match {
					it.innsatsgruppe == null
				},
			)
		}
	}

	@Test
	fun `oppdaterInnsatsgruppe - har ikke aktiv oppfolgingsperiode, ikke innsatsgruppe - oppdaterer ikke`() {
		val bruker =
			TestData.lagNavBruker(
				oppfolgingsperioder =
					listOf(
						Oppfolgingsperiode(
							id = UUID.randomUUID(),
							startdato = LocalDateTime.now().minusYears(3),
							sluttdato = LocalDateTime.now().minusMonths(2),
						),
					),
				innsatsgruppe = null,
			)
		every { brukerRepository.get(bruker.id) } returns bruker
		mockExecuteWithoutResult(transactionTemplate)

		service.oppdaterInnsatsgruppe(bruker.id, InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS)

		verify(exactly = 0) { brukerRepository.upsert(any()) }
	}
}
