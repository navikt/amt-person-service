package no.nav.amt.person.service.controller.migrering

import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class MigreringRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	fun upsert(migrering: MigreringDbo) {
		val sql = """
			insert into migrering_diff (resurs_id, endepunkt, request_body, diff, error)
			values (:resursId, :endepunkt, cast(:requestBody as jsonb), cast(:diff as jsonb), :error)
			on conflict (resurs_id) do update set
				request_body = cast(:requestBody as jsonb),
				diff = cast(:diff as jsonb),
				error = :error,
				modified_at = current_timestamp
		""".trimIndent()

		val parameters = sqlParameters(
			"resursId" to migrering.resursId,
			"endepunkt" to migrering.endepunkt,
			"requestBody" to migrering.requestBody,
			"diff" to migrering.diff,
			"error" to migrering.error,
		)
		template.update(sql, parameters)
	}

	fun get(resursId: UUID): MigreringDbo? {
		val sql = """
			select * from migrering_diff where resurs_id = :resursId
		""".trimIndent()

		val parameters = sqlParameters("resursId" to resursId)

		return template.query(sql, parameters) { rs, _ ->
			MigreringDbo(
				resursId = rs.getUUID("resurs_id"),
				endepunkt = rs.getString("endepunkt"),
				requestBody = rs.getString("request_body"),
				diff = rs.getString("diff"),
				error = rs.getString("error"),
			)
		}.firstOrNull()

	}
}

data class MigreringDbo(
	val resursId: UUID,
	val endepunkt: String,
	val requestBody: String,
	val diff: String?,
	val error: String?,
)
