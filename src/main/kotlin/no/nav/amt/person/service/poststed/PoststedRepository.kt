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
                    FROM postinformasjon
                    where postnummer = :postnummer;
            """,
            mapOf("postnummer" to postnummer)
        ) { resultSet, _ ->
            resultSet.getString("poststed")
        }.firstOrNull()
    }

    fun oppdaterPoststed(oppdatertPostinformasjon: List<PostInformasjon>, sporingsId: UUID) {
        val postinfoFraDb = getAllePoststeder()
        if (postinfoFraDb.size == oppdatertPostinformasjon.size && postinfoFraDb.toHashSet() == oppdatertPostinformasjon.toHashSet()) {
            log.info("Ingen endringer for $sporingsId, avslutter...")
            return
        }

        val oppdatertPostinformasjonMap: HashMap<String, PostInformasjon> = HashMap(oppdatertPostinformasjon.associateBy { it.postnummer })
        val postinfoFraDbMap: HashMap<String, PostInformasjon> = HashMap(postinfoFraDb.associateBy { it.postnummer })

        val slettesfraDb = postinfoFraDbMap.filter { oppdatertPostinformasjonMap[it.key] == null }.keys
        val oppdateresIDb = oppdatertPostinformasjonMap.filter { postinfoFraDbMap[it.key] != it.value }

        slettesfraDb.forEach {
            namedParameterJdbcTemplate.update(
                """
            DELETE FROM postinformasjon
            where postnummer = :postnummer;
        """,
                mapOf("postnummer" to it)
            )
        }
        oppdateresIDb.forEach {
            namedParameterJdbcTemplate.update(
                """
            INSERT INTO postinformasjon(postnummer, poststed)
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

    fun getAllePoststeder(): List<PostInformasjon> {
        return namedParameterJdbcTemplate.query(
            """
            SELECT postnummer,
                    poststed
                    FROM postinformasjon;
            """,
        ) { resultSet, _ ->
            resultSet.toPostInformasjon()
        }
    }
}

private fun ResultSet.toPostInformasjon(): PostInformasjon =
	PostInformasjon(
		postnummer = getString("postnummer"),
		poststed = getString("poststed")
	)
