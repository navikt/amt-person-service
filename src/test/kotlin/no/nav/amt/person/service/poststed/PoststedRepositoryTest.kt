package no.nav.amt.person.service.poststed

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class PoststedRepositoryTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
	private val poststedRepository = PoststedRepository(jdbcTemplate)

	@BeforeEach
	fun before() {
		lagrePostnummerForTest(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("5341", "STRAUME"),
				Postnummer("5365", "TURØY"),
				Postnummer("5449", "BØMLO"),
				Postnummer("9609", "NORDRE SEILAND")
			)
		)
	}

	@AfterEach
	fun after() {
		jdbcTemplate.update("DELETE FROM postnummer", MapSqlParameterSource())
	}

	@Test
	fun `getPoststed - postnummer finnes - henter riktig poststed`() {
		val poststed = poststedRepository.getPoststed("5341")

		poststed shouldBe "STRAUME"
	}

	@Test
	fun `getPoststed - postnummer finnes ikke - returnerer null`() {
		val poststed = poststedRepository.getPoststed("0101")

		poststed shouldBe null
	}

	@Test
	fun `getAllePoststeder - poststeder finnes - henter alle poststeder`() {
		val allePoststeder = poststedRepository.getAllePoststeder()

		allePoststeder.size shouldBe 5
	}

	@Test
	fun `oppdaterPoststed - postnummer finnes i db men ikke i oppdatert liste - sletter poststed`() {
		poststedRepository.oppdaterPoststed(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("5365", "TURØY"),
				Postnummer("5449", "BØMLO"),
				Postnummer("9609", "NORDRE SEILAND")
			),
			UUID.randomUUID()
		)

		val allePoststeder = poststedRepository.getAllePoststeder()

		allePoststeder.size shouldBe 4
		allePoststeder.find { it.postnummer == "5341" } shouldBe null
	}

	@Test
	fun `oppdaterPoststed - postnummer finnes i oppdatert liste men ikke i db - legger til poststed`() {
		poststedRepository.oppdaterPoststed(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("0502", "OSLO"),
				Postnummer("5341", "STRAUME"),
				Postnummer("5365", "TURØY"),
				Postnummer("5449", "BØMLO"),
				Postnummer("9609", "NORDRE SEILAND")
			),
			UUID.randomUUID()
		)

		val allePoststeder = poststedRepository.getAllePoststeder()

		allePoststeder.size shouldBe 6
		allePoststeder.find { it.postnummer == "0502" }?.poststed shouldBe "OSLO"
	}

	@Test
	fun `oppdaterPoststed - flere endringer - sletter 1 poststed, lagrer 1 poststed, bytter navn for 1 poststed`() {
		poststedRepository.oppdaterPoststed(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("0502", "OSLO"),
				Postnummer("5341", "STRAUME"),
				Postnummer("5365", "TURØY"),
				Postnummer("9609", "SENJA")
			),
			UUID.randomUUID()
		)

		val allePoststeder = poststedRepository.getAllePoststeder()

		allePoststeder.size shouldBe 5
		allePoststeder.find { it.postnummer == "0502" }?.poststed shouldBe "OSLO"
		allePoststeder.find { it.postnummer == "5449" } shouldBe null
		allePoststeder.find { it.postnummer == "9609" }?.poststed shouldBe "SENJA"
	}

	@Test
	fun `oppdaterPoststed - ingen endringer - oppdaterer ingenting`() {
		val allePoststeder = poststedRepository.getAllePoststeder()

		poststedRepository.oppdaterPoststed(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("5341", "STRAUME"),
				Postnummer("5365", "TURØY"),
				Postnummer("5449", "BØMLO"),
				Postnummer("9609", "NORDRE SEILAND")
			),
			UUID.randomUUID()
		)

		val allePoststederOppdatert = poststedRepository.getAllePoststeder()
		allePoststederOppdatert shouldBe allePoststeder
	}

	private fun lagrePostnummerForTest(postnummer: List<Postnummer>) {
		postnummer.forEach {
			jdbcTemplate.update(
				"""
            INSERT INTO postnummer(postnummer, poststed)
            VALUES (:postnummer, :poststed);
        """,
				mapOf(
					"postnummer" to it.postnummer,
					"poststed" to it.poststed
				)
			)
		}
	}
}
