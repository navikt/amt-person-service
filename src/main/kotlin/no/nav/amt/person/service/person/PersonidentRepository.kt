package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonidentDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PersonidentRepository(
	val template: NamedParameterJdbcTemplate,
) {
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

	fun get(ident: String): PersonidentDbo? {
		val sql =
			"""
			select * from personident where ident = :ident
			""".trimIndent()

		val parameters = sqlParameters("ident" to ident)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun getAllForPerson(personId: UUID): List<PersonidentDbo> {
		val sql =
			"""
			select * from personident where person_id = :personId
			""".trimIndent()

		val parameters = sqlParameters("personId" to personId)

		return template.query(sql, parameters, rowMapper)
	}

	fun upsert(
		personId: UUID,
		identer: List<Personident>,
	) {
		if (identer.isEmpty()) return

		val sql =
			"""
			insert into personident(
				ident,
				person_id,
				type,
				historisk
			) values (
				:ident,
				:personId,
				:type,
				:historisk
			) on conflict (ident, person_id) do update set
				type = :type,
				historisk = :historisk,
				modified_at = current_timestamp
			""".trimIndent()

		val parameters =
			identer.map {
				sqlParameters(
					"ident" to it.ident,
					"personId" to personId,
					"historisk" to it.historisk,
					"type" to it.type.name,
				)
			}
		template.batchUpdate(sql, parameters.toTypedArray())
	}
}
