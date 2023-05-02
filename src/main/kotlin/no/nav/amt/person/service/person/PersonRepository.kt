package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
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
			"id" to person.id,
			"personIdent" to person.personIdent,
			"personIdentType" to person.personIdentType.toString(),
			"historiskeIdenter" to person.historiskeIdenter.toTypedArray(),
			"fornavn" to person.fornavn,
			"mellomnavn" to person.mellomnavn,
			"etternavn" to person.etternavn
		)

		template.update(sql, parameters)
	}

	fun getPersoner(identer: List<String>): List<PersonDbo> {
		if (identer.isEmpty()) return emptyList()

		val sql = """
			select *
			from person
			where person_ident in (:identer)
		""".trimIndent()

		val parameters = sqlParameters(
			"identer" to identer,
		)

		return template.query(sql, parameters, rowMapper)

	}

	fun oppdaterIdenter(
		id: UUID,
		gjeldendeIdent: String,
		gjeldendeIdentType: IdentType,
		historiskeIdenter: List<String>,
	) {
		val sql = """
			update person
			set person_ident = :gjeldendeIdent,
				person_ident_type = :gjeldendeIdentType,
				historiske_identer = :historiskeIdenter,
				modified_at = current_timestamp
			where id = :id
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to id,
			"gjeldendeIdent" to gjeldendeIdent,
			"gjeldendeIdentType" to gjeldendeIdentType.toString(),
			"historiskeIdenter" to historiskeIdenter.toTypedArray(),
		)

		template.update(sql, parameters)
	}

}

