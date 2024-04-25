package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.getNullableUUID
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import no.nav.amt.person.service.utils.toPGObject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class NavBrukerRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		NavBrukerDbo(
			id = rs.getUUID("nav_bruker.id"),
			person = PersonDbo(
				id = rs.getUUID("nav_bruker.person_id"),
				personident = rs.getString("person.personident"),
				fornavn = rs.getString("person.fornavn"),
				mellomnavn = rs.getString("person.mellomnavn"),
				etternavn = rs.getString("person.etternavn"),
				createdAt = rs.getTimestamp("person.created_at").toLocalDateTime(),
				modifiedAt = rs.getTimestamp("person.modified_at").toLocalDateTime(),
			),
			navVeileder = rs.getNullableUUID("nav_bruker.nav_veileder_id")?.let {
				NavAnsattDbo(
					id = rs.getUUID("nav_bruker.nav_veileder_id"),
					navIdent = rs.getString("nav_ansatt.nav_ident"),
					navn = rs.getString("nav_ansatt.navn"),
					telefon = rs.getString("nav_ansatt.telefon"),
					epost = rs.getString("nav_ansatt.epost"),
					createdAt = rs.getTimestamp("nav_ansatt.created_at").toLocalDateTime(),
					modifiedAt = rs.getTimestamp("nav_ansatt.modified_at").toLocalDateTime(),
				)

			},
			navEnhet = rs.getNullableUUID("nav_bruker.nav_enhet_id")?.let {
				NavEnhetDbo(
					id = rs.getUUID("nav_bruker.nav_enhet_id"),
					enhetId = rs.getString("nav_enhet.nav_enhet_id"),
					navn = rs.getString("nav_enhet.navn"),
					createdAt = rs.getTimestamp("nav_enhet.created_at").toLocalDateTime(),
					modifiedAt = rs.getTimestamp("nav_enhet.modified_at").toLocalDateTime(),
				)
			},
			telefon = rs.getString("nav_bruker.telefon"),
			epost = rs.getString("nav_bruker.epost"),
			erSkjermet = rs.getBoolean("nav_bruker.er_skjermet"),
			adresse = rs.getString("nav_bruker.adresse")?.let { fromJsonString<Adresse>(it) },
			sisteKrrSync = rs.getTimestamp("siste_krr_sync")?.toLocalDateTime(),
			createdAt = rs.getTimestamp("nav_bruker.created_at").toLocalDateTime(),
			modifiedAt = rs.getTimestamp("nav_bruker.modified_at").toLocalDateTime(),
			adressebeskyttelse = rs.getString("nav_bruker.adressebeskyttelse")?.let { Adressebeskyttelse.valueOf(it) },
			oppfolgingsperioder = rs.getString("nav_bruker.oppfolgingsperioder")?.let { fromJsonString<List<Oppfolgingsperiode>>(it) } ?: emptyList(),
			innsatsgruppe = rs.getString("nav_bruker.innsatsgruppe")?.let { Innsatsgruppe.valueOf(it) }
		)
	}

	fun	get(id: UUID): NavBrukerDbo {
		val sql = selectNavBrukerQuery("where nav_bruker.id = :id")
		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).first()
	}

	fun	get(personident: String): NavBrukerDbo? {
		val sql = selectNavBrukerQuery(
			"""
				left join personident on nav_bruker.person_id = personident.person_id
				where personident.ident = :personident
			""".trimIndent()
		)
		val parameters = sqlParameters("personident" to personident)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun getByPersonId(personId: UUID): NavBrukerDbo? {
		val sql = selectNavBrukerQuery("where person_id = :personId")
		val parameters = sqlParameters("personId" to personId)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}


	fun	getAll(offset: Int, limit: Int, notSyncedSince: LocalDateTime? = null): List<NavBrukerDbo> {
		val sql = selectNavBrukerQuery("""
			WHERE (siste_krr_sync is null OR siste_krr_sync < :notSyncedSince)
			ORDER BY siste_krr_sync asc nulls first, nav_bruker.modified_at
			OFFSET :offset
			LIMIT :limit
			""")
		val parameters = sqlParameters("offset" to offset, "limit" to limit, "notSyncedSince" to notSyncedSince)

		return template.query(sql, parameters, rowMapper)
	}

	fun	getPersonidenter(offset: Int, limit: Int, notSyncedSince: LocalDateTime? = null): List<String> {
		val sql = """
			SELECT person.personident AS "person.personident"
			FROM nav_bruker
					 INNER JOIN person ON nav_bruker.person_id = person.id
			WHERE (siste_krr_sync is null OR siste_krr_sync < :notSyncedSince)
			ORDER BY siste_krr_sync asc nulls first, nav_bruker.modified_at
			OFFSET :offset
			LIMIT :limit
			"""
		val parameters = sqlParameters("offset" to offset, "limit" to limit, "notSyncedSince" to notSyncedSince)

		return template.query(sql, parameters) { rs, _ -> rs.getString("person.personident") }
	}

	fun upsert(bruker: NavBrukerUpsert) {
		val sql = """
			insert into nav_bruker(
				id,
				person_id,
				nav_veileder_id,
				nav_enhet_id,
				telefon,
				epost,
				er_skjermet,
				adresse,
				siste_krr_sync,
				adressebeskyttelse,
				oppfolgingsperioder,
				innsatsgruppe
			) values (
				:id,
				:personId,
				:navVeilederId,
				:navEnhetId,
				:telefon,
				:epost,
				:erSkjermet,
				:adresse,
				:sisteKrrSync,
				:adressebeskyttelse,
				:oppfolgingsperioder,
				:innsatsgruppe
			) on conflict(person_id) do update set
				nav_veileder_id = :navVeilederId,
				nav_enhet_id = :navEnhetId,
				telefon = :telefon,
				epost = :epost,
				er_skjermet = :erSkjermet,
				adresse = :adresse,
				siste_krr_sync = :sisteKrrSync,
				adressebeskyttelse = :adressebeskyttelse,
				oppfolgingsperioder = :oppfolgingsperioder,
				innsatsgruppe = :innsatsgruppe,
				modified_at = current_timestamp
				where nav_bruker.id = :id
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to bruker.id,
			"personId" to bruker.personId,
			"navVeilederId" to bruker.navVeilederId,
			"navEnhetId" to bruker.navEnhetId,
			"telefon" to bruker.telefon,
			"epost" to bruker.epost,
			"erSkjermet" to bruker.erSkjermet,
			"adresse" to toPGObject(bruker.adresse),
			"sisteKrrSync" to bruker.sisteKrrSync,
			"adressebeskyttelse" to bruker.adressebeskyttelse?.name,
			"oppfolgingsperioder" to toPGObject(bruker.oppfolgingsperioder),
			"innsatsgruppe" to bruker.innsatsgruppe?.name
		)

		template.update(sql, parameters)

	}

	private fun selectNavBrukerQuery(where: String): String {
		return """
			select nav_bruker.id as "nav_bruker.id",
				   nav_bruker.person_id as "nav_bruker.person_id",
				   nav_bruker.nav_veileder_id as "nav_bruker.nav_veileder_id",
				   nav_bruker.nav_enhet_id as "nav_bruker.nav_enhet_id",
				   nav_bruker.telefon as "nav_bruker.telefon",
				   nav_bruker.epost as "nav_bruker.epost",
				   nav_bruker.er_skjermet as "nav_bruker.er_skjermet",
				   nav_bruker.adresse as "nav_bruker.adresse",
				   nav_bruker.adressebeskyttelse as "nav_bruker.adressebeskyttelse",
				   nav_bruker.oppfolgingsperioder as "nav_bruker.oppfolgingsperioder",
				   nav_bruker.innsatsgruppe as "nav_bruker.innsatsgruppe",
				   nav_bruker.siste_krr_sync,
				   nav_bruker.created_at as "nav_bruker.created_at",
				   nav_bruker.modified_at as "nav_bruker.modified_at",
				   person.personident as "person.personident",
				   person.fornavn as "person.fornavn",
				   person.mellomnavn as "person.mellomnavn",
				   person.etternavn as "person.etternavn",
				   person.created_at as "person.created_at",
				   person.modified_at as "person.modified_at",
				   nav_ansatt.nav_ident as "nav_ansatt.nav_ident",
				   nav_ansatt.navn as "nav_ansatt.navn",
				   nav_ansatt.telefon as "nav_ansatt.telefon",
				   nav_ansatt.epost as "nav_ansatt.epost",
				   nav_ansatt.created_at as "nav_ansatt.created_at",
				   nav_ansatt.modified_at as "nav_ansatt.modified_at",
				   nav_enhet.nav_enhet_id as "nav_enhet.nav_enhet_id",
				   nav_enhet.navn as "nav_enhet.navn",
				   nav_enhet.created_at as "nav_enhet.created_at",
				   nav_enhet.modified_at as "nav_enhet.modified_at"
			from nav_bruker
					 left join person on nav_bruker.person_id = person.id
					 left join nav_ansatt on nav_bruker.nav_veileder_id = nav_ansatt.id
					 left join nav_enhet on nav_bruker.nav_enhet_id = nav_enhet.id
			$where
		""".trimIndent()
	}

	fun finnBrukerId(personident: String): UUID? {
		val sql = """
			select nb.id as "nav_bruker.id"
			from nav_bruker nb join person p on nb.person_id = p.id join personident ident on p.id = ident.person_id
			where ident.ident = :personident
			""".trimMargin()

		val parameters = sqlParameters("personident" to personident)

		return template.query(sql, parameters) {rs, _ -> rs.getNullableUUID("nav_bruker.id")}.firstOrNull()
	}

	fun delete(id: UUID) {
		val sql = """
			delete from nav_bruker
			where id = :id
		""".trimIndent()

		val parameters = sqlParameters("id" to id)

		template.update(sql, parameters)
	}

}

