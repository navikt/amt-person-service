package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonidentDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PersonidentRepository(
	val template: NamedParameterJdbcTemplate,
) {
	fun get(ident: String): PersonidentDbo? =
		template
			.query(
				"SELECT * FROM personident WHERE ident = :ident",
				sqlParameters("ident" to ident),
				rowMapper,
			).firstOrNull()

	fun getAllForPerson(personId: UUID): List<PersonidentDbo> =
		template.query(
			"SELECT * FROM personident WHERE person_id = :personId",
			sqlParameters("personId" to personId),
			rowMapper,
		)

	fun upsert(identer: Set<PersonidentDbo>) {
		if (identer.isEmpty()) return

		val sql =
			"""
			INSERT INTO personident (
				ident,
				person_id,
				type,
				historisk
			)
			VALUES (
				:ident,
				:personId,
				:type,
				:historisk
			)
			ON CONFLICT (ident, person_id) DO UPDATE SET
				type = :type,
				historisk = :historisk,
				modified_at = CURRENT_TIMESTAMP
			""".trimIndent()

		val parameters =
			identer.map {
				sqlParameters(
					"ident" to it.ident,
					"personId" to it.personId,
					"historisk" to it.historisk,
					"type" to it.type.name,
				)
			}

		template.batchUpdate(sql, parameters.toTypedArray())
	}

	fun getPersonIderMedFlerePersonidenter(): List<UUID> {
		val sql =
			"""
			SELECT DISTINCT person_id
			FROM personident
			WHERE
				historisk IS TRUE
				AND type = 'FOLKEREGISTERIDENT'
				AND modified_at > '2025-01-01'
				-- Antakelsen er at vi har reprodusert Nav-brukere etter denne datoen så vi kan snevre inn søket
			""".trimIndent()

		return template.query(sql) { rs, _ -> rs.getUUID("person_id") }
	}

	companion object {
		private val rowMapper =
			RowMapper { rs, _ ->
				PersonidentDbo(
					ident = rs.getString("ident"),
					personId = rs.getUUID("person_id"),
					historisk = rs.getBoolean("historisk"),
					type = rs.getString("type")?.let { IdentType.valueOf(it) } ?: IdentType.UKJENT,
					createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
					modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime(),
				)
			}
	}
}
