package no.nav.amt.person.service.integration.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.internal.PersonUpdater
import no.nav.amt.person.service.navansatt.NavAnsattUpdater
import no.nav.amt.person.service.navenhet.NavEnhetUpdateJob
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@AutoConfigureMockMvc
class InternalControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val navEnhetUpdateJob: NavEnhetUpdateJob,
    @MockkBean private val personUpdater: PersonUpdater,
    @MockkBean private val navAnsattUpdater: NavAnsattUpdater,
) : IntegrationTestBase() {
    @BeforeEach
    fun setupInternalMocks() {
        justRun { navEnhetUpdateJob.update() }
        justRun { personUpdater.oppdaterPersonidenter(any()) }
        justRun { navAnsattUpdater.oppdaterAlle() }
    }

    // MockMvc defaults to remoteAddr = "127.0.0.1", so requests are treated as internal by default.
    private fun MockHttpServletRequestDsl.fraEksternAdresse() {
        with { request ->
            request.remoteAddr = "10.0.0.1"
            request
        }
    }

    @Nested
    inner class Autentisering {
        @Test
        fun `skal returnere 401 for alle GET-endepunkter som ikke er interne`() {
            val endepunkter = listOf(
                "/internal/nav-brukere/republiser",
                "/internal/nav-brukere/oppdater-adr-republiser",
                "/internal/nav-bruker/oppdater-adr-republiser/${UUID.randomUUID()}",
                "/internal/nav-brukere/oppdater-innsats-republiser",
                "/internal/nav-bruker/oppdater-innsats-republiser/${UUID.randomUUID()}",
                "/internal/nav-brukere/republiser/${UUID.randomUUID()}",
                "/internal/arrangor-ansatte/republiser",
                "/internal/nav-ansatte/republiser",
                "/internal/nav-ansatte/oppdater",
                "/internal/nav-enhet/oppdater",
                "/internal/nav-brukere/synkroniser-krr",
                "/internal/nav-brukere/oppdater-manglende-kontaktinfo",
                "/internal/nav-brukere/republiser-ny-ident",
            )

            endepunkter.forEach { endpoint ->
                mockMvc
                    .get(endpoint) {
                        fraEksternAdresse()
                        contentType = MediaType.APPLICATION_JSON
                        content = "{}"
                    }.andExpect { status { isUnauthorized() } }
            }
        }
    }

    @Nested
    inner class OppdaterPersonidenter {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .post("/internal/person/identer")
                .andExpect { status { isOk() } }

            verify { personUpdater.oppdaterPersonidenter(0) }
        }

        @Test
        fun `skal ikke kjøre jobb når request kommer fra ekstern adresse`() {
            mockMvc
                .post("/internal/person/identer") {
                    fraEksternAdresse()
                }.andExpect { status { isOk() } }

            verify(exactly = 0) { personUpdater.oppdaterPersonidenter(any()) }
        }

        @Test
        fun `skal bruke offset-parameter`() {
            mockMvc
                .post("/internal/person/identer") {
                    param("offset", "100")
                }.andExpect { status { isOk() } }

            verify { personUpdater.oppdaterPersonidenter(100) }
        }
    }

    @Nested
    inner class OppdaterNavn {
        val person = TestData.lagPerson()

        @Test
        fun `skal returnere 200 og oppdatere navn for intern request`() {
            testDataRepository.insertPerson(person)
            every { pdlClient.hentPerson(person.personident) } returns TestData.lagPdlPerson(person)

            mockMvc
                .get("/internal/person/navn/${person.id}")
                .andExpect { status { isOk() } }
        }

        @Test
        fun `skal returnere 200 uten å oppdatere navn for ekstern request`() {
            testDataRepository.insertPerson(person)

            mockMvc
                .get("/internal/person/navn/${person.id}") {
                    fraEksternAdresse()
                }.andExpect { status { isOk() } }

            verify(exactly = 0) { pdlClient.hentPerson(any()) }
        }
    }

    @Nested
    inner class RepubliserNavBrukere {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/republiser")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class OppdaterOgRepubliserNavBrukere {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/oppdater-adr-republiser")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class OppdaterAdresseOgRepubliserNavBruker {
        @Test
        fun `skal returnere 200 for intern request`() {
            val navBruker = TestData.lagNavBruker()
            testDataRepository.insertNavBruker(navBruker)
            every { pdlClient.hentPerson(navBruker.person.personident) } returns TestData.lagPdlPerson(navBruker.person)

            mockMvc
                .get("/internal/nav-bruker/oppdater-adr-republiser/${navBruker.id}")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class OppdaterInnsatsOgRepubliser {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/oppdater-innsats-republiser")
                .andExpect { status { isOk() } }
        }

        @Test
        fun `skal returnere 200 for enkelt Nav-bruker`() {
            val navBruker = TestData.lagNavBruker()
            testDataRepository.insertNavBruker(navBruker)
            every { veilarboppfolgingClient.hentOppfolgingperioder(navBruker.person.personident) } returns
                navBruker.oppfolgingsperioder
            every { veilarbvedtaksstotteClient.hentInnsatsgruppe(navBruker.person.personident) } returns
                navBruker.innsatsgruppe

            mockMvc
                .get("/internal/nav-bruker/oppdater-innsats-republiser/${navBruker.id}")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class RepubliserNavBruker {
        @Test
        fun `skal returnere 200 for intern request`() {
            val navBruker = TestData.lagNavBruker()
            testDataRepository.insertNavBruker(navBruker)

            mockMvc
                .get("/internal/nav-brukere/republiser/${navBruker.id}")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class RepubliserArrangorAnsatte {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/arrangor-ansatte/republiser")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class RepubliserNavAnsatte {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-ansatte/republiser")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class OppdaterNavAnsatte {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-ansatte/oppdater")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class OppdaterNavEnheter {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-enhet/oppdater")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class SynkroniserKrr {
        val navBruker = TestData.lagNavBruker()

        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/synkroniser-krr")
                .andExpect { status { isOk() } }
        }

        @Test
        fun `skal returnere 401 for ekstern request`() {
            mockMvc
                .post("/internal/nav-brukere/synkroniser-krr") {
                    fraEksternAdresse()
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personident": "${navBruker.person.personident}"}"""
                }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `skal synkronisere kontaktinfo for enkelt Nav-bruker`() {
            testDataRepository.insertNavBruker(navBruker)
            every { krrProxyClient.hentKontaktinformasjon(navBruker.person.personident) } returns
                Result.success(
                    Kontaktinformasjon(
                        epost = navBruker.epost,
                        telefonnummer = navBruker.telefon,
                    ),
                )

            mockMvc
                .post("/internal/nav-brukere/synkroniser-krr") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personident": "${navBruker.person.personident}"}"""
                }.andExpect { status { isOk() } }
        }

        @Test
        fun `skal returnere 500 når Nav-bruker ikke finnes`() {
            mockMvc
                .post("/internal/nav-brukere/synkroniser-krr") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personident": "${TestData.randomIdent()}"}"""
                }.andExpect { status { isInternalServerError() } }
        }
    }

    @Nested
    inner class OppdaterManglendeKontaktinfo {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/oppdater-manglende-kontaktinfo")
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class RepubliserNavBrukereMedNyIdent {
        @Test
        fun `skal returnere 200 for intern request`() {
            mockMvc
                .get("/internal/nav-brukere/republiser-ny-ident")
                .andExpect { status { isOk() } }
        }
    }
}
