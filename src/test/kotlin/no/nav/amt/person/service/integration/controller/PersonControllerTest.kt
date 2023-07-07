package no.nav.amt.person.service.integration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.clients.amt_tiltak.BrukerInfoDto
import no.nav.amt.person.service.controller.dto.ArrangorAnsattDto
import no.nav.amt.person.service.controller.dto.NavAnsattDto
import no.nav.amt.person.service.controller.dto.NavBrukerDto
import no.nav.amt.person.service.controller.dto.NavEnhetDto
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.mock.servers.MockKontaktinformasjon
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class PersonControllerTest: IntegrationTestBase() {

	private val objectMapper = ObjectMapper().registerKotlinModule()

	@Autowired
	lateinit var personService: PersonService

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Autowired
	lateinit var navAnsattService: NavAnsattService

	@Autowired
	lateinit var navEnhetService: NavEnhetService

	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt finnes ikke - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val token = mockOAuthServer.issueAzureAdM2MToken()

		mockAmtTiltakHttpServer.mockHentBrukerId(person.personIdent, null)
		mockPdlHttpServer.mockHentPerson(person)

		val response = sendRequest(
			method = "POST",
			path = "/api/arrangor-ansatt",
			body = """{"personIdent": "${person.personIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer $token")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body!!.string())
		val faktiskPerson = personService.hentPerson(person.personIdent)!!

		faktiskPerson.id shouldBe body.id
		faktiskPerson.personIdent shouldBe body.personIdent
		faktiskPerson.fornavn shouldBe body.fornavn
		faktiskPerson.mellomnavn shouldBe body.mellomnavn
		faktiskPerson.etternavn shouldBe body.etternavn
	}

	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt er har bruker i amt-tiltak - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val brukerId = UUID.randomUUID()

		mockAmtTiltakHttpServer.mockHentBrukerId(person.personIdent, BrukerInfoDto(
			brukerId,
			UUID.randomUUID(),
			person.personIdentType,
			person.historiskeIdenter,
		))

		mockPdlHttpServer.mockHentPerson(person)
		val token = mockOAuthServer.issueAzureAdM2MToken()

		val response = sendRequest(
			method = "POST",
			path = "/api/arrangor-ansatt",
			body = """{"personIdent": "${person.personIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer $token")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body!!.string())
		val faktiskPerson = personService.hentPerson(person.personIdent)!!

		faktiskPerson.id shouldBe brukerId
		faktiskPerson.personIdent shouldBe body.personIdent
		faktiskPerson.fornavn shouldBe body.fornavn
		faktiskPerson.mellomnavn shouldBe body.mellomnavn
		faktiskPerson.etternavn shouldBe body.etternavn
	}

	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt er navBruker - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val navBruker = TestData.lagNavBruker(person = person)
		testDataRepository.insertNavBruker(navBruker)

		val token = mockOAuthServer.issueAzureAdM2MToken()

		val response = sendRequest(
			method = "POST",
			path = "/api/arrangor-ansatt",
			body = """{"personIdent": "${person.personIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer $token")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body!!.string())
		val faktiskPerson = personService.hentPerson(person.personIdent)!!

		faktiskPerson.id shouldBe body.id
		faktiskPerson.personIdent shouldBe body.personIdent
		faktiskPerson.fornavn shouldBe body.fornavn
		faktiskPerson.mellomnavn shouldBe body.mellomnavn
		faktiskPerson.etternavn shouldBe body.etternavn
	}


	@Test
	fun `hentEllerOpprettNavBruker - nav bruker finnes ikke - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()
		val navEnhet = TestData.lagNavEnhet()
		val navBruker = TestData.lagNavBruker(navVeileder = navAnsatt, navEnhet = navEnhet)

		mockPdlHttpServer.mockHentPerson(navBruker.person)
		mockVeilarboppfolgingHttpServer.mockHentVeilederIdent(navBruker.person.personIdent, navAnsatt.navIdent)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personIdent, navEnhet.enhetId)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(MockKontaktinformasjon(navBruker.epost, navBruker.telefon))
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personIdent to false))
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())
		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-bruker",
			body = """{"personIdent": "${navBruker.person.personIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavBrukerDto>(response.body!!.string())
		val faktiskBruker = navBrukerService.hentNavBruker(navBruker.person.personIdent)!!

		faktiskBruker.person.id shouldBe body.personId
		faktiskBruker.person.personIdent shouldBe body.personIdent
		faktiskBruker.person.fornavn shouldBe body.fornavn
		faktiskBruker.person.mellomnavn shouldBe body.mellomnavn
		faktiskBruker.person.etternavn shouldBe body.etternavn
		faktiskBruker.telefon shouldBe body.telefon
		faktiskBruker.epost shouldBe body.epost
		faktiskBruker.navVeileder!!.id shouldBe body.navVeilederId
		faktiskBruker.navEnhet!!.id shouldBe body.navEnhet!!.id
		faktiskBruker.navEnhet!!.enhetId shouldBe body.navEnhet!!.enhetId
		faktiskBruker.navEnhet!!.navn shouldBe body.navEnhet!!.navn
		faktiskBruker.erSkjermet shouldBe body.erSkjermet

		val ident = personService.hentIdenter(faktiskBruker.person.id).first()
		ident.ident shouldBe body.personIdent
		ident.type shouldBe IdentType.FOLKEREGISTERIDENT
		ident.historisk shouldBe false
	}

	@Test
	fun `hentEllerOpprettNavBruker - nav bruker er adressebeskyttet - skal ha status 500`() {
		val navBruker = TestData.lagNavBruker()

		mockPdlHttpServer.mockHentPerson(
			navBruker.person.personIdent,
			TestData.lagPdlPerson(
				person = navBruker.person,
				adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG
			)
		)

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-bruker",
			body = """{"personIdent": "${navBruker.person.personIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 500
	}

	@Test
	fun `hentEllerOpprettNavAnsatt - nav ansatt finnes ikke - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()

		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-ansatt",
			body = """{"navIdent": "${navAnsatt.navIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavAnsattDto>(response.body!!.string())
		val faktiskNavAnsatt = navAnsattService.hentNavAnsatt(navAnsatt.navIdent)!!

		faktiskNavAnsatt.id shouldBe body.id
		faktiskNavAnsatt.navIdent shouldBe body.navIdent
		faktiskNavAnsatt.navn shouldBe body.navn
		faktiskNavAnsatt.telefon shouldBe body.telefon
		faktiskNavAnsatt.epost shouldBe body.epost
	}

	@Test
	fun `hentEllerOpprettNavEnhet - nav enhet finnes ikke - skal ha status 200 og returnere riktig response`() {
		val navEnhet = TestData.lagNavEnhet()

		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-enhet",
			body = """{"enhetId": "${navEnhet.enhetId}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavEnhetDto>(response.body!!.string())
		val faktiskNavEnhet = navEnhetService.hentNavEnhet(navEnhet.enhetId)!!

		faktiskNavEnhet.id shouldBe body.id
		faktiskNavEnhet.enhetId shouldBe body.enhetId
		faktiskNavEnhet.navn shouldBe body.navn
	}

	@Test
	internal fun `skal teste token autentisering`() {
		val requestBuilders = listOf(
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/arrangor-ansatt"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-ansatt"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-bruker"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-enhet"),
		)
		testTokenAutentisering(requestBuilders)
	}

	fun testTokenAutentisering(requestBuilders: List<Request.Builder>) {
		requestBuilders.forEach {
			val utenTokenResponse = client.newCall(it.build()).execute()
			utenTokenResponse.code shouldBe 401
			val feilTokenResponse = client.newCall(it.header(
				name = "Authorization",
				value = "Bearer ${mockOAuthServer.issueToken(issuer = "ikke-azuread")}")
				.build()
			).execute()
			feilTokenResponse.code shouldBe 401
		}
	}

}
