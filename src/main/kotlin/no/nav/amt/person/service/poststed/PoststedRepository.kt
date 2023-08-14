package no.nav.amt.person.service.poststed

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.UUID

@Transactional
@Component
class PoststedRepository(
	private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getPoststed(postnummer: String): String? {
		return namedParameterJdbcTemplate.query(
			"""
				SELECT poststed
				FROM postnummer
				where postnummer = :postnummer;
            """,
			mapOf("postnummer" to postnummer)
		) { resultSet, _ ->
			resultSet.getString("poststed")
		}.firstOrNull()
	}

	fun oppdaterPoststed(oppdatertePostnummer: List<Postnummer>, sporingsId: UUID) {
		val postnummerFraDb = getAllePoststeder()
		if (postnummerFraDb.size == oppdatertePostnummer.size && postnummerFraDb.toHashSet() == oppdatertePostnummer.toHashSet()) {
			log.info("Ingen endringer for $sporingsId, avslutter...")
			return
		}

		val oppdatertePostnummerMap: HashMap<String, Postnummer> =
			HashMap(oppdatertePostnummer.associateBy { it.postnummer })
		val postnummerFraDbMap: HashMap<String, Postnummer> = HashMap(postnummerFraDb.associateBy { it.postnummer })

		val slettesfraDb = postnummerFraDbMap.filter { oppdatertePostnummerMap[it.key] == null }.keys
		val oppdateresIDb = oppdatertePostnummerMap.filter { postnummerFraDbMap[it.key] != it.value }

		slettesfraDb.forEach {
			namedParameterJdbcTemplate.update(
				"""
					DELETE FROM postnummer
					where postnummer = :postnummer;
					""",
				mapOf("postnummer" to it)
			)
		}
		oppdateresIDb.forEach {
			namedParameterJdbcTemplate.update(
				"""
					INSERT INTO postnummer(postnummer, poststed)
					VALUES (:postnummer, :poststed)
					ON CONFLICT (postnummer) DO UPDATE SET poststed = :poststed;
					""",
				mapOf(
					"postnummer" to it.key,
					"poststed" to it.value.poststed
				)
			)
		}
	}

	fun getAllePoststeder(): List<Postnummer> {
		return namedParameterJdbcTemplate.query(
			"""
				SELECT postnummer,
				poststed
				FROM postnummer;
				""",
		) { resultSet, _ ->
			resultSet.toPostnummer()
		}
	}
}

private fun ResultSet.toPostnummer(): Postnummer =
	Postnummer(
		postnummer = getString("postnummer"),
		poststed = getString("poststed")
	)
