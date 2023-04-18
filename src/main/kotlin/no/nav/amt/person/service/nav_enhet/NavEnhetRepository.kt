package no.nav.amt.person.service.nav_enhet

import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class NavEnhetRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		NavEnhetDbo(
			id = rs.getUUID("id"),
			enhetId = rs.getString("nav_enhet_id"),
			navn = rs.getString("navn"),
			createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
			modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime(),
		)
	}

	fun insert(input: NavEnhet) {
		val sql = """
			INSERT INTO nav_enhet(id, nav_enhet_id, navn) VALUES (:id, :enhetId, :navn)
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to input.id,
			"enhetId" to input.enhetId,
			"navn" to input.navn
		)

		template.update(sql, parameters)
	}

	fun get(id: UUID): NavEnhetDbo {
		val sql = """
			SELECT * FROM nav_enhet WHERE id = :id
		""".trimIndent()

		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).firstOrNull()
			?: throw NoSuchElementException("Enhet med id $id eksisterer ikke.")
	}

	fun get(enhetId: String): NavEnhetDbo? {
		val sql = """
			SELECT * FROM nav_enhet WHERE nav_enhet_id = :enhetId
		""".trimIndent()

		return template.query(
			sql,
			sqlParameters("enhetId" to enhetId),
			rowMapper
		).firstOrNull()
	}

}
