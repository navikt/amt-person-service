package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.dbo.PersonUpsert
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class PersonRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val rowMapper = RowMapper { rs, _ ->
		PersonDbo(
			id = rs.getUUID("id"),
			personIdent = rs.getString("person_ident"),
			personIdentType = rs.getString("person_ident_type")?.let { IdentType.valueOf(it)},
			historiskeIdenter = (rs.getArray("historiske_identer").array as Array<String>).asList(),
			fornavn = rs.getString("fornavn"),
			mellomnavn = rs.getString("mellomnavn"),
			etternavn = rs.getString("etternavn"),
			createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
			modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime()
		)
	}
	fun get(id: UUID): PersonDbo {
		val sql = "select * from person where id = :id"
		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).first()
	}

	fun get(personIdent: String): PersonDbo? {
		val sql = "select * from person where person_ident = :personIdent"
		val parameters = sqlParameters("personIdent" to personIdent)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun upsert(personUpsert: PersonUpsert) {
		val sql = """
			insert into person(
				id,
				person_ident,
				person_ident_type,
				historiske_identer,
				fornavn,
				mellomnavn,
				etternavn
			) values (
				:id,
				:personIdent,
				:personIdentType,
				:historiskeIdenter,
				:fornavn,
				:mellomnavn,
				:etternavn
			) on conflict(person_ident) do update set
				fornavn = :fornavn,
				mellomnavn = :mellomnavn,
				etternavn = :etternavn,
				historiske_identer = :historiskeIdenter,
				modified_at = current_timestamp
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to personUpsert.id,
			"personIdent" to personUpsert.personIdent,
			"personIdentType" to personUpsert.personIdentType.toString(),
			"historiskeIdenter" to personUpsert.historiskeIdenter.toTypedArray(),
			"fornavn" to personUpsert.fornavn,
			"mellomnavn" to personUpsert.mellomnavn,
			"etternavn" to personUpsert.etternavn
		)

		template.update(sql, parameters)
	}

}

