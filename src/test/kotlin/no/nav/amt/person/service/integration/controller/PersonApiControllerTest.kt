package no.nav.amt.person.service.integration.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.amt.person.service.api.dto.AdressebeskyttelseDto
import no.nav.amt.person.service.api.dto.ArrangorAnsattDto
import no.nav.amt.person.service.api.dto.NavAnsattDto
import no.nav.amt.person.service.api.dto.NavBrukerDto
import no.nav.amt.person.service.api.dto.NavBrukerFodselsdatoDto
import no.nav.amt.person.service.api.dto.NavEnhetDto
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.clients.norg.NorgNavEnhetDto
import no.nav.amt.person.service.clients.oppfolgingskontor.Arbeidsoppfolging
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.navansatt.NavAnsattRepository
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.NavBrukerDbo
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navenhet.NavEnhetRepository
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonidentRepository
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@AutoConfigureMockMvc
class PersonApiControllerTest(
    private val mockMvc: MockMvc,
    private val personidentRepository: PersonidentRepository,
    private val personRepository: PersonRepository,
    private val navBrukerRepository: NavBrukerRepository,
    private val navAnsattRepository: NavAnsattRepository,
    private val navEnhetRepository: NavEnhetRepository,
    private val mockOAuth2Server: MockOAuth2Server,
) : IntegrationTestBase() {
    private fun m2mToken() = issueAzureAdM2MToken()

    private fun mockNavBrukerAvhengigheter(navBruker: NavBrukerDbo) {
        every { pdlClient.hentPerson(navBruker.person.personident) } returns TestData.lagPdlPerson(navBruker.person)
        every { pdlClient.hentTelefon(navBruker.person.personident) } returns navBruker.telefon

        every { veilarboppfolgingClient.hentVeilederIdent(navBruker.person.personident) } returns
            (navBruker.navVeileder?.navIdent)

        every { veilarboppfolgingClient.hentOppfolgingperioder(navBruker.person.personident) } returns
            navBruker.oppfolgingsperioder

        every { veilarbvedtaksstotteClient.hentInnsatsgruppe(navBruker.person.personident) } returns
            navBruker.innsatsgruppe

        every { oppfolgingskontorClient.hentKontorForBruker(navBruker.person.personident) } returns
            navBruker.navEnhet?.let { Arbeidsoppfolging(kontorId = it.enhetId, kontorNavn = it.navn) }

        every { krrProxyClient.hentKontaktinformasjon(navBruker.person.personident) } returns
            Result.success(Kontaktinformasjon(epost = navBruker.epost, telefonnummer = navBruker.telefon))

        every { poaoTilgangClient.erSkjermetPerson(navBruker.person.personident) } returns
            ApiResult(result = navBruker.erSkjermet, throwable = null)

        if (navBruker.navVeileder != null) {
            every { nomClient.hentNavAnsatt(navBruker.navVeileder.navIdent) } returns NomNavAnsatt(
                navIdent = navBruker.navVeileder.navIdent,
                navn = navBruker.navVeileder.navn,
                telefonnummer = navBruker.navVeileder.telefon,
                epost = navBruker.navVeileder.epost,
                orgTilknytning = TestData.orgTilknytning,
            )
        }

        if (navBruker.navEnhet != null) {
            every { norgClient.hentNavEnhet(navBruker.navEnhet.enhetId) } returns
                NorgNavEnhetDto.fromDbo(navBruker.navEnhet)
        }

        // NavAnsattService needs norg to resolve the ansatt's enhet
        every { norgClient.hentNavEnhet(TestData.navGrunerlokka.enhetId) } returns
            NorgNavEnhetDto.fromDbo(TestData.navGrunerlokka)
    }

    private fun sammenlignBrukerDtoer(
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
            erSkjermet shouldBe brukerDto.erSkjermet
            adressebeskyttelse shouldBe brukerDto.adressebeskyttelse
            oppfolgingsperioder shouldBe brukerDto.oppfolgingsperioder
            innsatsgruppe shouldBe brukerDto.innsatsgruppe
        }
    }

    @Nested
    inner class Autentisering {
        @Test
        fun `skal avvise request uten token`() {
            mockMvc
                .perform(post("/api/nav-bruker").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `skal avvise request med token fra feil issuer`() {
            val feilToken = mockOAuth2Server.issueToken(issuerId = "ikke-azuread").serialize()

            mockMvc
                .perform(
                    post("/api/nav-bruker")
                        .header("Authorization", "Bearer $feilToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `skal avvise azure ad token som ikke er M2M`() {
            val nonM2MToken = issueAzureAdToken()

            mockMvc
                .perform(
                    post("/api/nav-bruker")
                        .header("Authorization", "Bearer $nonM2MToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `skal avvise alle endepunkter uten token`() {
            val endpoints = listOf(
                post("/api/arrangor-ansatt"),
                post("/api/nav-ansatt"),
                post("/api/nav-bruker"),
                post("/api/nav-bruker-fodselsar"),
                post("/api/nav-bruker/kontaktinformasjon"),
                post("/api/nav-enhet"),
                get("/api/nav-ansatt/${UUID.randomUUID()}"),
                get("/api/nav-enhet/${UUID.randomUUID()}"),
            )

            endpoints.forEach { request ->
                mockMvc
                    .perform(request.contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    inner class ArrangorAnsatt {
        @Test
        fun `ansatt finnes ikke - skal opprette og returnere`() {
            val person = TestData.lagPerson()
            every { pdlClient.hentPerson(person.personident) } returns TestData.lagPdlPerson(person)

            val result = mockMvc
                .perform(
                    post("/api/arrangor-ansatt")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "${person.personident}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<ArrangorAnsattDto>(result.response.contentAsString)
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
        fun `ansatt er navBruker - skal returnere person`() {
            val person = TestData.lagPerson()
            val navBruker = TestData.lagNavBruker(person = person)
            testDataRepository.insertNavBruker(navBruker)

            val result = mockMvc
                .perform(
                    post("/api/arrangor-ansatt")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "${person.personident}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<ArrangorAnsattDto>(result.response.contentAsString)
            val faktiskPerson = personRepository.get(person.personident)

            assertSoftly(faktiskPerson.shouldNotBeNull()) {
                id shouldBe body.id
                personident shouldBe body.personident
                fornavn shouldBe body.fornavn
                mellomnavn shouldBe body.mellomnavn
                etternavn shouldBe body.etternavn
            }
        }
    }

    @Nested
    inner class NavBruker {
        @Test
        fun `bruker finnes ikke - skal opprette og returnere`() {
            val navAnsatt = TestData.lagNavAnsatt()
            val navEnhet = TestData.lagNavEnhet()
            val navBruker = TestData.lagNavBruker(navVeileder = navAnsatt, navEnhet = navEnhet)

            mockNavBrukerAvhengigheter(navBruker)

            val result = mockMvc
                .perform(
                    post("/api/nav-bruker")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "${navBruker.person.personident}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavBrukerDto>(result.response.contentAsString)
            val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

            sammenlignBrukerDtoer(faktiskBruker.shouldNotBeNull(), body)

            val ident = personidentRepository.getAllForPerson(faktiskBruker.person.id).first()

            assertSoftly(ident) {
                it.ident shouldBe body.personident
                type shouldBe IdentType.FOLKEREGISTERIDENT
                historisk shouldBe false
            }
        }

        @Test
        fun `bruker finnes - skal returnere eksisterende`() {
            val navBruker = TestData.lagNavBruker()
            testDataRepository.insertNavBruker(navBruker)

            val result = mockMvc
                .perform(
                    post("/api/nav-bruker")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "${navBruker.person.personident}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val navBrukerDto = objectMapper.readValue<NavBrukerDto>(result.response.contentAsString)
            val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

            sammenlignBrukerDtoer(faktiskBruker.shouldNotBeNull(), navBrukerDto)
        }

        @Test
        fun `bruker er adressebeskyttet - skal returnere med beskyttelse`() {
            val navAnsatt = TestData.lagNavAnsatt()
            val navEnhet = TestData.lagNavEnhet()
            val navBruker = TestData.lagNavBruker(navVeileder = navAnsatt, navEnhet = navEnhet)

            every { pdlClient.hentPerson(navBruker.person.personident) } returns TestData.lagPdlPerson(
                person = navBruker.person,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            )
            every { pdlClient.hentTelefon(navBruker.person.personident) } returns navBruker.telefon
            every { veilarboppfolgingClient.hentVeilederIdent(navBruker.person.personident) } returns navAnsatt.navIdent
            every { veilarboppfolgingClient.hentOppfolgingperioder(navBruker.person.personident) } returns
                navBruker.oppfolgingsperioder
            every { veilarbvedtaksstotteClient.hentInnsatsgruppe(navBruker.person.personident) } returns
                InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS
            every { oppfolgingskontorClient.hentKontorForBruker(navBruker.person.personident) } returns
                Arbeidsoppfolging(kontorId = navEnhet.enhetId, kontorNavn = navEnhet.navn)
            every { krrProxyClient.hentKontaktinformasjon(navBruker.person.personident) } returns
                Result.success(Kontaktinformasjon(epost = navBruker.epost, telefonnummer = navBruker.telefon))
            every { poaoTilgangClient.erSkjermetPerson(navBruker.person.personident) } returns
                ApiResult(result = false, throwable = null)
            every { nomClient.hentNavAnsatt(navAnsatt.navIdent) } returns NomNavAnsatt(
                navIdent = navAnsatt.navIdent,
                navn = navAnsatt.navn,
                telefonnummer = navAnsatt.telefon,
                epost = navAnsatt.epost,
                orgTilknytning = TestData.orgTilknytning,
            )
            every { norgClient.hentNavEnhet(navEnhet.enhetId) } returns NorgNavEnhetDto.fromDbo(navEnhet)
            every { norgClient.hentNavEnhet(TestData.navGrunerlokka.enhetId) } returns
                NorgNavEnhetDto.fromDbo(TestData.navGrunerlokka)

            val result = mockMvc
                .perform(
                    post("/api/nav-bruker")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "${navBruker.person.personident}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val navBrukerDto = objectMapper.readValue<NavBrukerDto>(result.response.contentAsString)
            val faktiskBruker = navBrukerRepository.get(navBruker.person.personident)

            sammenlignBrukerDtoer(faktiskBruker.shouldNotBeNull(), navBrukerDto)
        }
    }

    @Nested
    inner class NavBrukerFodselsar {
        @Test
        fun `skal returnere fodselsar fra PDL`() {
            val personident = TestData.randomIdent()
            val forventetFodselsar = 1990

            every { pdlClient.hentPersonFodselsar(personident) } returns forventetFodselsar

            val result = mockMvc
                .perform(
                    post("/api/nav-bruker-fodselsar")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "$personident"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavBrukerFodselsdatoDto>(result.response.contentAsString)
            body.fodselsar shouldBe forventetFodselsar
        }
    }

    @Nested
    inner class NavBrukerKontaktinformasjon {
        @Test
        fun `skal returnere kontaktinformasjon for personidenter`() {
            val navBruker = TestData.lagNavBruker()
            testDataRepository.insertNavBruker(navBruker)

            every { krrProxyClient.hentKontaktinformasjon(setOf(navBruker.person.personident)) } returns
                Result.success(
                    mapOf(
                        navBruker.person.personident to Kontaktinformasjon(
                            epost = navBruker.epost,
                            telefonnummer = navBruker.telefon,
                        ),
                    ),
                )
            every { pdlClient.hentTelefon(navBruker.person.personident) } returns navBruker.telefon

            val result = mockMvc
                .perform(
                    post("/api/nav-bruker/kontaktinformasjon")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staticObjectMapper.writeValueAsString(setOf(navBruker.person.personident))),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<Map<String, Kontaktinformasjon>>(result.response.contentAsString)
            body[navBruker.person.personident].shouldNotBeNull()
        }
    }

    @Nested
    inner class NavAnsatt {
        @Test
        fun `ansatt er ikke lagret - skal opprette og returnere`() {
            val navAnsatt = TestData.lagNavAnsatt()

            every { nomClient.hentNavAnsatt(navAnsatt.navIdent) } returns NomNavAnsatt(
                navIdent = navAnsatt.navIdent,
                navn = navAnsatt.navn,
                telefonnummer = navAnsatt.telefon,
                epost = navAnsatt.epost,
                orgTilknytning = TestData.orgTilknytning,
            )
            every { norgClient.hentNavEnhet(TestData.navGrunerlokka.enhetId) } returns
                NorgNavEnhetDto.fromDbo(TestData.navGrunerlokka)

            val result = mockMvc
                .perform(
                    post("/api/nav-ansatt")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"navIdent": "${navAnsatt.navIdent}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavAnsattDto>(result.response.contentAsString)
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
        fun `ansatt finnes - skal returnere eksisterende`() {
            val navAnsatt = TestData.lagNavAnsatt()
            testDataRepository.insertNavAnsatt(navAnsatt)

            val result = mockMvc
                .perform(
                    get("/api/nav-ansatt/${navAnsatt.id}")
                        .header("Authorization", "Bearer ${m2mToken()}"),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavAnsattDto>(result.response.contentAsString)

            assertSoftly(body) {
                id shouldBe navAnsatt.id
                navIdent shouldBe navAnsatt.navIdent
                navn shouldBe navAnsatt.navn
                telefon shouldBe navAnsatt.telefon
                epost shouldBe navAnsatt.epost
            }
        }
    }

    @Nested
    inner class NavEnhet {
        @Test
        fun `enhet finnes ikke - skal opprette og returnere`() {
            val navEnhet = TestData.lagNavEnhet()

            every { norgClient.hentNavEnhet(navEnhet.enhetId) } returns NorgNavEnhetDto.fromDbo(navEnhet)

            val result = mockMvc
                .perform(
                    post("/api/nav-enhet")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"enhetId": "${navEnhet.enhetId}"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavEnhetDto>(result.response.contentAsString)
            val faktiskNavEnhet = navEnhetRepository.get(navEnhet.enhetId)

            assertSoftly(faktiskNavEnhet.shouldNotBeNull()) {
                id shouldBe body.id
                enhetId shouldBe body.enhetId
                navn shouldBe body.navn
            }
        }

        @Test
        fun `enhet finnes - skal returnere eksisterende`() {
            val navEnhet = TestData.lagNavEnhet()
            testDataRepository.insertNavEnhet(navEnhet)

            val result = mockMvc
                .perform(
                    get("/api/nav-enhet/${navEnhet.id}")
                        .header("Authorization", "Bearer ${m2mToken()}"),
                ).andExpect(status().isOk)
                .andReturn()

            val body = objectMapper.readValue<NavEnhetDto>(result.response.contentAsString)

            assertSoftly(body) {
                id shouldBe navEnhet.id
                enhetId shouldBe navEnhet.enhetId
                navn shouldBe navEnhet.navn
            }
        }
    }

    @Nested
    inner class Adressebeskyttelse {
        @Test
        fun `person er beskyttet - skal returnere gradering`() {
            val personident = TestData.randomIdent()
            val gradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG

            every { pdlClient.hentAdressebeskyttelse(personident) } returns gradering

            val result = mockMvc
                .perform(
                    post("/api/person/adressebeskyttelse")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "$personident"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            objectMapper.readValue<AdressebeskyttelseDto>(result.response.contentAsString).gradering shouldBe gradering
        }

        @Test
        fun `person er ikke beskyttet - skal returnere null gradering`() {
            val personident = TestData.randomIdent()

            every { pdlClient.hentAdressebeskyttelse(personident) } returns null

            val result = mockMvc
                .perform(
                    post("/api/person/adressebeskyttelse")
                        .header("Authorization", "Bearer ${m2mToken()}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"personident": "$personident"}"""),
                ).andExpect(status().isOk)
                .andReturn()

            objectMapper.readValue<AdressebeskyttelseDto>(result.response.contentAsString).gradering shouldBe null
        }
    }

    fun issueAzureAdM2MToken(
        subject: String = UUID.randomUUID().toString(),
        audience: String = "test-aud",
    ): String {
        val claims = mapOf(
            "roles" to arrayOf("access_as_application"),
            "oid" to subject,
        )
        return mockOAuth2Server
            .issueToken(
                issuerId = "azuread",
                subject = subject,
                audience = audience,
                claims = claims,
            ).serialize()
    }

    fun issueAzureAdToken(
        subject: String = UUID.randomUUID().toString(),
        audience: String = "test-aud",
    ): String {
        val claims = mapOf(
            "oid" to UUID.randomUUID().toString(),
        )
        return mockOAuth2Server
            .issueToken(
                issuerId = "azuread",
                subject = subject,
                audience = audience,
                claims = claims,
            ).serialize()
    }
}
