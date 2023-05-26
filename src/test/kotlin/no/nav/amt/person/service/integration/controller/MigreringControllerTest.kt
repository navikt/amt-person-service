package no.nav.amt.person.service.integration.controller

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.controller.migrering.MigreringNavEnhet
import no.nav.amt.person.service.controller.migrering.MigreringRepository
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MigreringControllerTest: IntegrationTestBase() {

	@Autowired
	lateinit var navEnhetService: NavEnhetService

	@Autowired
	lateinit var migreringRepository: MigreringRepository

	@Test
	fun `migrerNavEnhet - samme enhetId ulike navn - skal opprette diff`() {
		val navEnhet = TestData.lagNavEnhet(navn = "Nytt navn")
		val request = MigreringNavEnhet(navEnhet.id, navEnhet.enhetId, "Gammelt navn")

		mockNorgHttpServer.mockHentNavEnhet(navEnhet.toModel())

		val body = JsonUtils.toJsonString(request)

		val response = sendRequest(
			method = "POST",
			path = "/api/migrer/nav-enhet",
			body = body.toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
		)

		response.code shouldBe 200

		val diff = migreringRepository.get(navEnhet.id)

		diff.endepunkt shouldBe "nav-enhet"
		diff.diff shouldBe """{"navn": {"amtPerson": "Nytt navn", "amtTiltak": "Gammelt navn"}}"""

		val opprettetEnhet = navEnhetService.hentNavEnhet(request.id)
		opprettetEnhet.enhetId shouldBe request.enhetId
		opprettetEnhet.navn shouldBe navEnhet.navn
	}
}
