package no.nav.amt.person.service.data

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class TestDataRepository(
	private val template: NamedParameterJdbcTemplate
) {
	fun insertPerson(person: PersonDbo) {
		val sql = """
			insert into person(
				id,
				person_ident,
				person_ident_type,
				historiske_identer,
				fornavn,
				mellomnavn,
				etternavn,
				created_at,
				modified_at
			) values (
				:id,
				:personIdent,
				:personIdentType,
				:historiskeIdenter,
				:fornavn,
				:mellomnavn,
				:etternavn,
				:createdAt,
				:modifiedAt
			)
		""".trimIndent()

		template.update(
			sql, sqlParameters(
				"id" to person.id,
				"personIdent" to person.personIdent,
				"personIdentType" to person.personIdentType.toString(),
				"historiskeIdenter" to person.historiskeIdenter.toTypedArray(),
				"fornavn" to person.fornavn,
				"mellomnavn" to person.mellomnavn,
				"etternavn" to person.etternavn,
				"createdAt" to person.createdAt,
				"modifiedAt" to person.modifiedAt,
			)
		)
	}
}
