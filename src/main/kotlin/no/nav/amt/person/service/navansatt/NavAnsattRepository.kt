package no.nav.amt.person.service.navansatt

import no.nav.amt.person.service.utils.getNullableUUID
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NavAnsattRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper =
		RowMapper { rs, _ ->
			NavAnsattDbo(
				id = rs.getUUID("id"),
				navIdent = rs.getString("nav_ident"),
				navn = rs.getString("navn"),
				telefon = rs.getString("telefon"),
				epost = rs.getString("epost"),
				navEnhetId = rs.getNullableUUID("nav_enhet_id"),
				createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
				modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime(),
			)
		}

	fun get(id: UUID): NavAnsattDbo {
		val sql =
			"""
			select * from nav_ansatt where id = :id
			""".trimIndent()

		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).first()
	}

	fun get(navIdent: String): NavAnsattDbo? {
		val sql =
			"""
			select * from nav_ansatt where nav_ident = :navIdent
			""".trimIndent()

		val parameters = sqlParameters("navIdent" to navIdent)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun getAll(): List<NavAnsattDbo> {
		val sql =
			"""
			select * from nav_ansatt
			""".trimIndent()

		return template.jdbcTemplate.query(sql, rowMapper)
	}

	fun upsert(navAnsatt: NavAnsatt): NavAnsattDbo {
		val parameters =
			sqlParameters(
				"id" to navAnsatt.id,
				"navIdent" to navAnsatt.navIdent,
				"navn" to navAnsatt.navn,
				"telefon" to navAnsatt.telefon,
				"epost" to navAnsatt.epost,
				"nav_enhet_id" to navAnsatt.navEnhetId,
			)

		return template.query("$upsertSql returning *", parameters, rowMapper).first()
	}

	fun upsertMany(ansatte: List<NavAnsatt>) {
		val parameters =
			ansatte.map { navAnsatt ->
				sqlParameters(
					"id" to navAnsatt.id,
					"navIdent" to navAnsatt.navIdent,
					"navn" to navAnsatt.navn,
					"telefon" to navAnsatt.telefon,
					"epost" to navAnsatt.epost,
					"nav_enhet_id" to navAnsatt.navEnhetId,
				)
			}

		template.batchUpdate(upsertSql, parameters.toTypedArray())
	}

	private val upsertSql =
		"""
		insert into nav_ansatt(id, nav_ident, navn, telefon, epost, nav_enhet_id)
		values (:id, :navIdent, :navn, :telefon, :epost, :nav_enhet_id)
		on conflict (nav_ident) do update set
			navn = :navn,
			telefon = :telefon,
			epost = :epost,
			modified_at = current_timestamp,
			nav_enhet_id = :nav_enhet_id
		""".trimIndent()
}
