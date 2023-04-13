package no.nav.amt.person.service.integration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class IntegrationTest : IntegrationTestBase() {

	@Test
	internal fun livenessCheck() {
		val response = sendRequest(
			method = "GET",
			path = "/internal/health/liveness"
		)
		response.code shouldBe 200
	}

	@Test
	internal fun flywayMigrationCheck() {
		val template = NamedParameterJdbcTemplate(dataSource)
		template.query(
			"select count(*) antall_migreringer from flyway_schema_history"
		) {
			it.getInt("antall_migreringer") shouldBe 2
		}

		template.query(
			"select count(*) antall_tabeller from information_schema.tables where table_schema = 'public'"
		) {
			it.getInt("antall_tabeller") shouldBe 6
		}
	}

}
