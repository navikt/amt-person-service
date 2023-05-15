package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerKontaktinfo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.getNullableUUID
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class NavBrukerRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		NavBrukerDbo(
			id = rs.getUUID("nav_bruker.id"),
			person = PersonDbo(
				id = rs.getUUID("nav_bruker.person_id"),
				personIdent = rs.getString("person.person_ident"),
				personIdentType = rs.getString("person.person_ident_type")?.let { IdentType.valueOf(it) },
				historiskeIdenter = (rs.getArray("person.historiske_identer").array as Array<String>).asList(),
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
			createdAt = rs.getTimestamp("nav_bruker.created_at").toLocalDateTime(),
			modifiedAt = rs.getTimestamp("nav_bruker.modified_at").toLocalDateTime()
		)
	}

	fun	get(id: UUID): NavBrukerDbo {
		val sql = selectNavBrukerQuery("where nav_bruker.id = :id")
		val parameters = sqlParameters("id" to id)

		return template.query(sql, parameters, rowMapper).first()
	}

	fun	get(personIdent: String): NavBrukerDbo? {
		val sql = selectNavBrukerQuery("where person.person_ident = :personIdent")
		val parameters = sqlParameters("personIdent" to personIdent)

		return template.query(sql, parameters, rowMapper).firstOrNull()
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
				er_skjermet
			) values (
				:id,
				:personId,
				:navVeilederId,
				:navEnhetId,
				:telefon,
				:epost,
				:erSkjermet
			) on conflict(id) do update set
				nav_veileder_id = :navVeilederId,
				nav_enhet_id = :navEnhetId,
				telefon = :telefon,
				epost = :epost,
				er_skjermet = :erSkjermet,
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
				   nav_bruker.created_at as "nav_bruker.created_at",
				   nav_bruker.modified_at as "nav_bruker.modified_at",
				   person.person_ident as "person.person_ident",
				   person.fornavn as "person.fornavn",
				   person.mellomnavn as "person.mellomnavn",
				   person.etternavn as "person.etternavn",
				   person.historiske_identer as "person.historiske_identer",
				   person.person_ident_type as "person.person_ident_type",
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

	fun finnBrukerId(personIdent: String): UUID? {
		val sql = """
			select nb.id as "nav_bruker.id"
			from nav_bruker nb join person p on nb.person_id = p.id
			where p.person_ident = :personIdent
			""".trimMargin()

		val parameters = sqlParameters("personIdent" to personIdent)

		return template.query(sql, parameters) {rs, _ -> rs.getNullableUUID("nav_bruker.id")}.firstOrNull()
	}

	fun oppdaterNavVeileder(navBrukerId: UUID, veilederId: UUID) {
		val sql = """
			update nav_bruker
			set nav_veileder_id = :veilederId,
				modified_at = current_timestamp
			where id = :navBrukerId
		""".trimIndent()

		val parameters = sqlParameters(
			"navBrukerId" to navBrukerId,
			"veilederId" to veilederId,
		)

		template.update(sql, parameters)
	}

	fun settSkjermet(navBrukerId: UUID, erSkjermet: Boolean) {
		val sql = """
			update nav_bruker
			set er_skjermet = :erSkjermet,
				modified_at = current_timestamp
			where id = :navBrukerId
		""".trimIndent()

		val parameters = sqlParameters(
			"navBrukerId" to navBrukerId,
			"erSkjermet" to erSkjermet,
		)

		template.update(sql, parameters)
	}

	fun oppdaterKontaktinformasjon(kontaktinfo: NavBrukerKontaktinfo) {
		val sql = """
			update nav_bruker
			set telefon = :telefon,
				epost = :epost,
				modified_at = current_timestamp
			where id = :navBrukerId
		""".trimIndent()

		val parameters = sqlParameters(
			"navBrukerId" to kontaktinfo.navBrukerId,
			"telefon" to kontaktinfo.telefon,
			"epost" to kontaktinfo.epost,
		)

		template.update(sql, parameters)
	}

	fun deleteByPersonId(personId: UUID) {
		val sql = """
			delete from nav_bruker
			where person_id = :personId
		""".trimIndent()

		val parameters = sqlParameters("personId" to personId)

		template.update(sql, parameters)
	}

	fun hentKontaktinformasjonHvisBrukerFinnes(personIdent: String): NavBrukerKontaktinfo? {
		val sql = """
			select nb.id as "nav_bruker.id",
				   nb.telefon as "nav_bruker.telefon",
				   nb.epost as "nav_bruker.epost"
			from nav_bruker nb join person p on nb.person_id = p.id
			where p.person_ident = :personIdent
			""".trimMargin()

		val parameters = sqlParameters("personIdent" to personIdent)

		return template.query(sql, parameters) {rs, _ -> NavBrukerKontaktinfo(
			navBrukerId = rs.getUUID("nav_bruker.id"),
			telefon = rs.getString("nav_bruker.telefon"),
			epost = rs.getString("nav_bruker.epost"),
		)
		}.firstOrNull()
	}
}

