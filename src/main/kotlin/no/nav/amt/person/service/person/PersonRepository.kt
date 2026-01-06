package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PersonRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper =
		RowMapper { rs, _ ->
			PersonDbo(
				id = rs.getUUID("id"),
				personident = rs.getString("personident"),
				fornavn = rs.getString("fornavn"),
				mellomnavn = rs.getString("mellomnavn"),
				etternavn = rs.getString("etternavn"),
				createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
				modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime(),
			)
		}

	fun get(id: UUID): PersonDbo {
		val sql = "select * from person where id = :id"
		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).first()
	}

	fun get(personident: String): PersonDbo? {
		val sql =
			"""
			select * from person join personident ident on person.id = ident.person_id
			where ident.ident = :personident
			""".trimMargin()
		val parameters = sqlParameters("personident" to personident)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun upsert(person: Person) {
		val sql =
			"""
			insert into person(
				id,
				personident,
				fornavn,
				mellomnavn,
				etternavn
			) values (
				:id,
				:personident,
				:fornavn,
				:mellomnavn,
				:etternavn
			) on conflict(id) do update set
				fornavn = :fornavn,
				mellomnavn = :mellomnavn,
				etternavn = :etternavn,
				personident = :personident,
				modified_at = current_timestamp
			""".trimIndent()

		val parameters =
			sqlParameters(
				"id" to person.id,
				"personident" to person.personident,
				"fornavn" to person.fornavn,
				"mellomnavn" to person.mellomnavn,
				"etternavn" to person.etternavn,
			)

		template.update(sql, parameters)
	}

	fun getPersoner(identer: List<String>): List<PersonDbo> {
		if (identer.isEmpty()) return emptyList()

		val sql =
			"""
			select *
			from person join personident ident on person.id = ident.person_id
			where ident.ident in (:identer)
			""".trimIndent()

		val parameters =
			sqlParameters(
				"identer" to identer,
			)

		return template.query(sql, parameters, rowMapper).distinct()
	}

	fun delete(id: UUID) {
		val parameters = sqlParameters("id" to id)

		val identSql =
			"""
			delete from personident where person_id = :id
			""".trimIndent()

		template.update(identSql, parameters)

		val sql =
			"""
			delete from person where id = :id
			""".trimIndent()

		template.update(sql, parameters)
	}

	fun getAll(
		offset: Int,
		limit: Int = 500,
	): List<PersonDbo> {
		val sql =
			"""
			select * from person
			order by id
			limit :limit
			offset :offset
			""".trimIndent()

		val parameters =
			sqlParameters(
				"offset" to offset,
				"limit" to limit,
			)

		return template.query(sql, parameters, rowMapper)
	}

	fun getAllWithRolle(
		offset: Int,
		limit: Int = 500,
		rolle: Rolle,
	): List<PersonDbo> {
		val sql =
			"""
			select person.* from person join person_rolle pr on person.id = pr.person_id
			where pr.type = :rolle
			order by person.id
			limit :limit
			offset :offset
			""".trimIndent()

		val parameters =
			sqlParameters(
				"offset" to offset,
				"limit" to limit,
				"rolle" to rolle.name,
			)

		return template.query(sql, parameters, rowMapper)
	}
}
