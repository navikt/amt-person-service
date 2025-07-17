package no.nav.amt.person.service.integration.controller

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.amt.person.service.integration.mock.servers.MockKontaktinformasjon
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.Innsatsgruppe
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import okhttp3.Request
import org.junit.jupiter.api.Test
import java.util.UUID

class PersonAPITest(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
	private val navAnsattService: NavAnsattService,
	private val navEnhetService: NavEnhetService,
) : IntegrationTestBase() {

	@Test
	fun `hentEllerOpprettArrangorAnsatt - ansatt finnes ikke - skal ha status 200 og returnere riktig response`() {
		val person = TestData.lagPerson()
		val token = mockOAuthServer.issueAzureAdM2MToken()

		mockPdlHttpServer.mockHentPerson(person)

		val response = sendRequest(
			method = "POST",
			path = "/api/arrangor-ansatt",
			body = """{"personident": "${person.personident}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer $token")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body.string())
		val faktiskPerson = personService.hentPerson(person.personident)

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

		val response = sendRequest(
			method = "POST",
			path = "/api/arrangor-ansatt",
			body = """{"personident": "${person.personident}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer $token")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<ArrangorAnsattDto>(response.body.string())
		val faktiskPerson = personService.hentPerson(person.personident)

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
			navBruker.oppfolgingsperioder
		)
		mockVeilarbvedtaksstotteHttpServer.mockHentInnsatsgruppe(
			navBruker.person.personident,
			Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE
		)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personident, navEnhet.enhetId)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(
			MockKontaktinformasjon(
				navBruker.person.personident,
				navBruker.epost,
				navBruker.telefon
			)
		)
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personident to false))
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())
		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())
		mockNorgHttpServer.addNavAnsattEnhet()

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-bruker",
			body = """{"personident": "${navBruker.person.personident}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerService.hentNavBruker(navBruker.person.personident)

		sammenlign(faktiskBruker.shouldNotBeNull(), body)

		val ident = personService.hentIdenter(faktiskBruker.person.id).first()
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

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-bruker",
			body = """{"personident": "${navBruker.person.personident}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val navBrukerDto = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerService.hentNavBruker(navBruker.person.personident)

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
				adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG
			)
		)
		mockVeilarboppfolgingHttpServer.mockHentVeilederIdent(navBruker.person.personident, navAnsatt.navIdent)
		mockVeilarboppfolgingHttpServer.mockHentOppfolgingperioder(
			navBruker.person.personident,
			navBruker.oppfolgingsperioder
		)
		mockVeilarbvedtaksstotteHttpServer.mockHentInnsatsgruppe(
			navBruker.person.personident,
			Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE
		)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personident, navEnhet.enhetId)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(
			MockKontaktinformasjon(
				navBruker.person.personident,
				navBruker.epost,
				navBruker.telefon
			)
		)
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personident to false))
		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())
		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())
		mockNorgHttpServer.addNavAnsattEnhet()

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-bruker",
			body = """{"personident": "${navBruker.person.personident}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val navBrukerDto = objectMapper.readValue<NavBrukerDto>(response.body.string())
		val faktiskBruker = navBrukerService.hentNavBruker(navBruker.person.personident)

		sammenlign(faktiskBruker.shouldNotBeNull(), navBrukerDto)
	}

	@Test
	fun `hentEllerOpprettNavAnsatt - nav ansatt er ikke lagret - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()

		mockNomHttpServer.mockHentNavAnsatt(navAnsatt.toModel())
		mockNorgHttpServer.addNavAnsattEnhet()

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-ansatt",
			body = """{"navIdent": "${navAnsatt.navIdent}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavAnsattDto>(response.body.string())
		val faktiskNavAnsatt = navAnsattService.hentNavAnsatt(navAnsatt.navIdent)

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

		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())

		val response = sendRequest(
			method = "POST",
			path = "/api/nav-enhet",
			body = """{"enhetId": "${navEnhet.enhetId}"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavEnhetDto>(response.body.string())
		val faktiskNavEnhet = navEnhetService.hentNavEnhet(navEnhet.enhetId)

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

		val response = sendRequest(
			method = "POST",
			path = "/api/person/adressebeskyttelse",
			body = """{"personident": "$personident"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		objectMapper.readValue<AdressebeskyttelseDto>(response.body.string()).gradering shouldBe gradering
	}

	@Test
	fun `hentAdressebeskyttelse - person er ikke beskyttet - skal ha status 200 og returnere riktig response`() {
		val personident = TestData.randomIdent()
		val gradering = null

		mockPdlHttpServer.mockHentAdressebeskyttelse(personident, gradering)

		val response = sendRequest(
			method = "POST",
			path = "/api/person/adressebeskyttelse",
			body = """{"personident": "$personident"}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		objectMapper.readValue<AdressebeskyttelseDto>(response.body.string()).gradering shouldBe gradering
	}

	@Test
	fun `hentNavAnsatt - nav ansatt finnes - skal ha status 200 og returnere riktig response`() {
		val navAnsatt = TestData.lagNavAnsatt()
		testDataRepository.insertNavAnsatt(navAnsatt)

		val response = sendRequest(
			method = "GET",
			path = "/api/nav-ansatt/${navAnsatt.id}",
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

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

		val response = sendRequest(
			method = "GET",
			path = "/api/nav-enhet/${navEnhet.id}",
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 200

		val body = objectMapper.readValue<NavEnhetDto>(response.body.string())

		assertSoftly(body) {
			id shouldBe navEnhet.id
			enhetId shouldBe navEnhet.enhetId
			navn shouldBe navEnhet.navn
		}
	}

	@Test
	internal fun `skal teste token autentisering`() {
		val requestBuilders = listOf(
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/arrangor-ansatt"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-ansatt"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-bruker"),
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/api/nav-enhet"),
			Request.Builder().get().url("${serverUrl()}/api/nav-ansatt/${UUID.randomUUID()}"),
			Request.Builder().get().url("${serverUrl()}/api/nav-enhet/${UUID.randomUUID()}")
		)
		testTokenAutentisering(requestBuilders)
	}

	fun testTokenAutentisering(requestBuilders: List<Request.Builder>) {
		requestBuilders.forEach {
			val utenTokenResponse = client.newCall(it.build()).execute()
			utenTokenResponse.code shouldBe 401
			val feilTokenResponse = client.newCall(
				it.header(
					name = "Authorization",
					value = "Bearer ${mockOAuthServer.issueToken(issuer = "ikke-azuread")}"
				)
					.build()
			).execute()
			feilTokenResponse.code shouldBe 401
		}
	}

	private fun sammenlign(
		faktiskBruker: NavBruker,
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

