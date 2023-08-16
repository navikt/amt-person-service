package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RolleRepository(
	private val template: NamedParameterJdbcTemplate
) {
	fun insert(personId: UUID, rolle: Rolle) {
		val sql = """
			insert into person_rolle(id, person_id, type)
			values(:id, :personId, :rolle)
			on conflict (person_id, type) do nothing
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to UUID.randomUUID(),
			"personId" to personId,
			"rolle" to rolle.name,
		)

		template.update(sql, parameters)
	}

	fun harRolle(personId: UUID, rolle: Rolle): Boolean {
		val sql = """
			select count(*)
			from person_rolle
			where person_id = :personId and type = :rolle
		""".trimIndent()

		val parameters = sqlParameters(
			"personId" to personId,
			"rolle" to rolle.name,
		)

		return template.queryForObject(sql, parameters, Int::class.java)!! > 0
	}

	fun delete(personId: UUID, rolle: Rolle) {
		val sql = """
			delete from person_rolle
			where person_id = :personId and type = :rolle
		""".trimIndent()

		val parameters = sqlParameters(
			"personId" to personId,
			"rolle" to rolle.name,
		)

		template.update(sql, parameters)
	}
}
