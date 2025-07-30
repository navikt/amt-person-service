package no.nav.amt.person.service.integration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IntegrationTest : IntegrationTestBase() {
	@Test
	internal fun livenessCheck() {
		val response =
			sendRequest(
				method = "GET",
				path = "/internal/health/liveness",
			)
		response.code shouldBe 200
	}
}
