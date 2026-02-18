package no.nav.amt.person.service.integration.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.api.dto.AdressebeskyttelseDto
import no.nav.amt.person.service.api.dto.ArrangorAnsattDto
import no.nav.amt.person.service.api.dto.NavAnsattDto
import no.nav.amt.person.service.api.dto.NavBrukerDto
import no.nav.amt.person.service.api.dto.NavEnhetDto
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.navansatt.NavAnsattRepository
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.NavBrukerDbo
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navenhet.NavEnhetRepository
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonidentRepository
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.amt.person.service.utils.StringUtils.emptyRequest
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.readValue
import java.util.UUID

class PersonApiControllerTest(
	private val personidentRepository: PersonidentRepository,
	private val personRepository: PersonRepository,
	private val navBrukerRepository: NavBrukerRepository,
	private val navAnsattRepository: NavAnsattRepository,
	private val navEnhetRepository: NavEnhetRepository,
) : IntegrationTestBase() {
	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt finnes ikke - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val token = mockOAuthServer.issueAzureAdM2MToken()

		mockPdlHttpServer.mockHentPerson(person)

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/arrangor-ansatt",
				body = """{"personident": "${person.personident}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $token"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body.string())
		val faktiskPerson = personRepository.get(person.personident)

		assertSoftly(faktiskPerson.shouldNotBeNull()) {
			id shouldBe body.id
			personident shouldBe body.personident
			fornavn shouldBe body.fornavn
			mellomnavn shouldBe body.mellomnavn
			etternavn shouldBe body.etternavn
		}
	}

	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt er navBruker - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val navBruker = TestData.lagNavBruker(person = person)
		testDataRepository.insertNavBruker(navBruker)

		val token = mockOAuthServer.issueAzureAdM2MToken()

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/arrangor-ansatt",
				body = """{"personident": "${person.personident}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $token"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body.string())
		val faktiskPerson = personRepository.get(person.personident)

		assertSoftly(faktiskPerson.shouldNotBeNull()) {
			id shouldBe body.id
			personident shouldBe body.personident
			fornavn shouldBe body.fornavn
			mellomnavn shouldBe body.mellomnavn
			etternavn shouldBe body.etternavn
		}
	}

	@Test
	fun `hentEllerOpprettNavBruker - nav bruker finnes ikke - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()
		val navEnhet = TestData.lagNavEnhet()
		val navBruker = TestData.lagNavBruker(navVeileder = navAnsatt, navEnhet = navEnhet)

		mockPdlHttpServer.mockHentPerson(navBruker.person)
		mockVeilarboppfolgingHttpServer.mockHentVeilederIdent(navBruker.person.personident, navAnsatt.navIdent)
		mockVeilarboppfolgingHttpServer.mockHentOppfolgingperioder(
			navBruker.person.personident,
			navBruker.oppfolgingsperioder,
		)
		mockVeilarbvedtaksstotteHttpServer.mockHentInnsatsgruppe(
			navBruker.person.personident,
			InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
		)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personident, navEnhet.enhetId)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(navBruker)
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personident to false))
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt)
		mockNorgHttpServer.addNavEnhet(navEnhet)
		mockNorgHttpServer.addNavEnhetGrunerLokka()

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/nav-bruker",
				body = """{"personident": "${navBruker.person.personident}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

		sammenlign(faktiskBruker.shouldNotBeNull(), body)

		val ident =
			personidentRepository
				.getAllForPerson(faktiskBruker.person.id)
				.first()

		assertSoftly(ident) {
			it.ident shouldBe body.personident
			type shouldBe IdentType.FOLKEREGISTERIDENT
			historisk shouldBe false
		}
	}

	@Test
	fun `hentEllerOpprettNavBruker - nav bruker finnes - skal ha status 200 og returnere riktig response`() {
		val navBruker = TestData.lagNavBruker()
		testDataRepository.insertNavBruker(navBruker)

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/nav-bruker",
				body = """{"personident": "${navBruker.person.personident}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val navBrukerDto = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

		sammenlign(faktiskBruker.shouldNotBeNull(), navBrukerDto)
	}

	@Test
	fun `hentEllerOpprettNavBruker - nav bruker er adressebeskyttet - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()
		val navEnhet = TestData.lagNavEnhet()
		val navBruker = TestData.lagNavBruker(navVeileder = navAnsatt, navEnhet = navEnhet)

		mockPdlHttpServer.mockHentPerson(
			navBruker.person.personident,
			TestData.lagPdlPerson(
				person = navBruker.person,
				adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
			),
		)
		mockVeilarboppfolgingHttpServer.mockHentVeilederIdent(navBruker.person.personident, navAnsatt.navIdent)
		mockVeilarboppfolgingHttpServer.mockHentOppfolgingperioder(
			navBruker.person.personident,
			navBruker.oppfolgingsperioder,
		)
		mockVeilarbvedtaksstotteHttpServer.mockHentInnsatsgruppe(
			navBruker.person.personident,
			InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
		)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personident, navEnhet.enhetId)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(navBruker)
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personident to false))
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt)
		mockNorgHttpServer.addNavEnhet(navEnhet)
		mockNorgHttpServer.addNavEnhetGrunerLokka()

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/nav-bruker",
				body = """{"personident": "${navBruker.person.personident}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val navBrukerDto = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

		sammenlign(faktiskBruker.shouldNotBeNull(), navBrukerDto)
	}

	@Test
	fun `hentEllerOpprettNavAnsatt - nav ansatt er ikke lagret - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()

		mockNomHttpServer.mockHentNavAnsatt(navAnsatt)
		mockNorgHttpServer.addNavEnhetGrunerLokka()

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/nav-ansatt",
				body = """{"navIdent": "${navAnsatt.navIdent}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<NavAnsattDto>(response.body.string())
		val faktiskNavAnsatt = navAnsattRepository.get(navAnsatt.navIdent)

		assertSoftly(faktiskNavAnsatt.shouldNotBeNull()) {
			id shouldBe body.id
			navIdent shouldBe body.navIdent
			navn shouldBe body.navn
			telefon shouldBe body.telefon
			epost shouldBe body.epost
		}
	}

	@Test
	fun `hentEllerOpprettNavEnhet - nav enhet finnes ikke - skal ha status 200 og returnere riktig response`() {
		val navEnhet = TestData.lagNavEnhet()

		mockNorgHttpServer.addNavEnhet(navEnhet)

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/nav-enhet",
				body = """{"enhetId": "${navEnhet.enhetId}"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<NavEnhetDto>(response.body.string())
		val faktiskNavEnhet = navEnhetRepository.get(navEnhet.enhetId)

		assertSoftly(faktiskNavEnhet.shouldNotBeNull()) {
			id shouldBe body.id
			enhetId shouldBe body.enhetId
			navn shouldBe body.navn
		}
	}

	@Test
	fun `hentAdressebeskyttelse - person er beskyttet - skal ha status 200 og returnere riktig response`() {
		val personident = TestData.randomIdent()
		val gradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG

		mockPdlHttpServer.mockHentAdressebeskyttelse(personident, gradering)

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/person/adressebeskyttelse",
				body = """{"personident": "$personident"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		objectMapper.readValue<AdressebeskyttelseDto>(response.body.string()).gradering shouldBe gradering
	}

	@Test
	fun `hentAdressebeskyttelse - person er ikke beskyttet - skal ha status 200 og returnere riktig response`() {
		val personident = TestData.randomIdent()
		val gradering = null

		mockPdlHttpServer.mockHentAdressebeskyttelse(personident, gradering)

		val response =
			sendRequest(
				method = HttpMethod.POST.name(),
				path = "/api/person/adressebeskyttelse",
				body = """{"personident": "$personident"}""".toRequestBody(mediaTypeJson),
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		objectMapper.readValue<AdressebeskyttelseDto>(response.body.string()).gradering shouldBe gradering
	}

	@Test
	fun `hentNavAnsatt - nav ansatt finnes - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()
		testDataRepository.insertNavAnsatt(navAnsatt)

		val response =
			sendRequest(
				method = "GET",
				path = "/api/nav-ansatt/${navAnsatt.id}",
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<NavAnsattDto>(response.body.string())

		assertSoftly(body) {
			id shouldBe navAnsatt.id
			navIdent shouldBe navAnsatt.navIdent
			navn shouldBe navAnsatt.navn
			telefon shouldBe navAnsatt.telefon
			epost shouldBe navAnsatt.epost
		}
	}

	@Test
	fun `hentNavEnhet - enhet finnes - skal ha status 200 og returnere riktig response`() {
		val navEnhet = TestData.lagNavEnhet()
		testDataRepository.insertNavEnhet(navEnhet)

		val response =
			sendRequest(
				method = "GET",
				path = "/api/nav-enhet/${navEnhet.id}",
				headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			)

		response.code shouldBe HttpStatus.OK.value()

		val body = objectMapper.readValue<NavEnhetDto>(response.body.string())

		assertSoftly(body) {
			id shouldBe navEnhet.id
			enhetId shouldBe navEnhet.enhetId
			navn shouldBe navEnhet.navn
		}
	}

	@Test
	fun `skal teste token autentisering`() {
		val requestBuilders =
			listOf(
				Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/arrangor-ansatt"),
				Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-ansatt"),
				Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-bruker"),
				Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-enhet"),
				Request.Builder().get().url("${serverUrl()}/api/nav-ansatt/${UUID.randomUUID()}"),
				Request.Builder().get().url("${serverUrl()}/api/nav-enhet/${UUID.randomUUID()}"),
			)
		testTokenAutentisering(requestBuilders)
	}

	fun testTokenAutentisering(requestBuilders: List<Request.Builder>) {
		requestBuilders.forEach {
			val utenTokenResponse = client.newCall(it.build()).execute()
			utenTokenResponse.code shouldBe 401
			val feilTokenResponse =
				client
					.newCall(
						it
							.header(
								name = HttpHeaders.AUTHORIZATION,
								value = "Bearer ${mockOAuthServer.issueToken(issuer = "ikke-azuread")}",
							).build(),
					).execute()
			feilTokenResponse.code shouldBe HttpStatus.UNAUTHORIZED.value()
		}
	}

	private fun sammenlign(
		faktiskBruker: NavBrukerDbo,
		brukerDto: NavBrukerDto,
	) {
		assertSoftly(faktiskBruker) {
			assertSoftly(person) {
				id shouldBe brukerDto.personId
				personident shouldBe brukerDto.personident
				fornavn shouldBe brukerDto.fornavn
				mellomnavn shouldBe brukerDto.mellomnavn
				etternavn shouldBe brukerDto.etternavn
			}

			telefon shouldBe brukerDto.telefon
			epost shouldBe brukerDto.epost
			navVeileder?.id shouldBe brukerDto.navVeilederId
			navEnhet?.id shouldBe brukerDto.navEnhet?.id
			navEnhet?.enhetId shouldBe brukerDto.navEnhet?.enhetId
			navEnhet?.navn shouldBe brukerDto.navEnhet?.navn
			telefon shouldBe brukerDto.telefon
			epost shouldBe brukerDto.epost
			erSkjermet shouldBe brukerDto.erSkjermet
			adressebeskyttelse shouldBe brukerDto.adressebeskyttelse
			oppfolgingsperioder shouldBe brukerDto.oppfolgingsperioder
			innsatsgruppe shouldBe brukerDto.innsatsgruppe
		}
	}
}
