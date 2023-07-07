package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import no.nav.amt.person.service.utils.titlecase
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

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

	fun get(personident: String): PersonDbo? {
		val sql = """
			select * from person
			where person_ident = :personident or :personident = any(historiske_identer)
		""".trimMargin()
		val parameters = sqlParameters("personident" to personident)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun upsert(person: Person) {
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
				:personident,
				:personidentType,
				:historiskeIdenter,
				:fornavn,
				:mellomnavn,
				:etternavn
			) on conflict(id) do update set
				fornavn = :fornavn,
				mellomnavn = :mellomnavn,
				etternavn = :etternavn,
				person_ident = :personident,
				person_ident_type = :personidentType,
				historiske_identer = :historiskeIdenter,
				modified_at = current_timestamp
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to person.id,
			"personident" to person.personIdent,
			"personidentType" to person.personIdentType.toString(),
			"historiskeIdenter" to person.historiskeIdenter.toTypedArray(),
			"fornavn" to person.fornavn.titlecase(),
			"mellomnavn" to person.mellomnavn?.titlecase(),
			"etternavn" to person.etternavn.titlecase(),
		)

		template.update(sql, parameters)
	}

	fun getPersoner(identer: List<String>): List<PersonDbo> {
		if (identer.isEmpty()) return emptyList()

		val sql = """
			select *
			from person
			where person_ident = any(:identer)
				or historiske_identer && cast(:identer as text[])
		""".trimIndent()

		val parameters = sqlParameters(
			"identer" to identer.toTypedArray(),
		)

		return template.query(sql, parameters, rowMapper)

	}

	fun delete(id: UUID) {
		val sql = """
			delete from person where id = :id
		""".trimIndent()

		val parameters = sqlParameters("id" to id)

		template.update(sql, parameters)
	}

	fun getAll(offset: Int, limit: Int = 500): List<PersonDbo> {
		val sql = """
			select * from person
			order by id
			limit :limit
			offset :offset
		""".trimIndent()

		val parameters = sqlParameters(
			"offset" to offset,
			"limit" to limit,
		)

		return template.query(sql, parameters, rowMapper)
	}

}

