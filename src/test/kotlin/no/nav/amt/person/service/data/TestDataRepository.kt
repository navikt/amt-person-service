package no.nav.amt.person.service.data

import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.utils.sqlParameters
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class TestDataRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val log = LoggerFactory.getLogger(javaClass)

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

	fun insertNavEnhet(enhet: NavEnhetDbo) {
		val sql = """
			INSERT INTO nav_enhet(id, nav_enhet_id, navn, created_at, modified_at)
			VALUES (:id, :enhetId, :navn, :createdAt, :modifiedAt)
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to enhet.id,
			"enhetId" to enhet.enhetId,
			"navn" to enhet.navn,
			"createdAt" to enhet.createdAt,
			"modifiedAt" to enhet.modifiedAt,
		)

		template.update(sql, parameters)
	}

	fun insertNavAnsatt(ansatt: NavAnsattDbo) {
		val sql = """
			insert into nav_ansatt(
				id,
				nav_ident,
				navn,
				telefon,
				epost,
				created_at,
				modified_at
			) values (
				:id,
				:navIdent,
				:navn,
				:telefon,
				:epost,
				:createdAt,
				:modifiedAt
			)
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to ansatt.id,
			"navIdent" to ansatt.navIdent,
			"navn" to ansatt.navn,
			"telefon" to ansatt.telefon,
			"epost" to ansatt.epost,
			"createdAt" to ansatt.createdAt,
			"modifiedAt" to ansatt.modifiedAt,
		)

		template.update(sql, parameters)
	}

	fun insertNavBruker(bruker: NavBrukerDbo) {
		try {
			insertPerson(bruker.person)
		} catch (e: DuplicateKeyException) {
			log.warn("Person med id ${bruker.person.id} er allerede opprettet")
		}
		try {
			if (bruker.navVeileder != null) {
				insertNavAnsatt(bruker.navVeileder!!)
			}
		} catch (e: DuplicateKeyException) {
			log.warn("Nav ansatt med id ${bruker.navVeileder!!.id} er allerede opprettet")
		}
		try {
			if (bruker.navEnhet != null) {
				insertNavEnhet(bruker.navEnhet!!)
			}
		} catch (e: DuplicateKeyException) {
			log.warn("Nav enhet med id ${bruker.navEnhet!!.id} er allerede opprettet")
		}

		val sql = """
			insert into nav_bruker(
				id,
				person_id,
				nav_veileder_id,
				nav_enhet_id,
				telefon,
				epost,
				er_skjermet,
				created_at,
				modified_at
			) values (
				:id,
				:personId,
				:navVeilederId,
				:navEnhetId,
				:telefon,
				:epost,
				:erSkjermet,
				:createdAt,
				:modifiedAt
			)
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to bruker.id,
			"personId" to bruker.person.id,
			"navVeilederId" to bruker.navVeileder?.id,
			"navEnhetId" to bruker.navEnhet?.id,
			"telefon" to bruker.telefon,
			"epost" to bruker.epost,
			"erSkjermet" to bruker.erSkjermet,
			"createdAt" to bruker.createdAt,
			"modifiedAt" to bruker.modifiedAt
		)

		template.update(sql, parameters)
	}
}
