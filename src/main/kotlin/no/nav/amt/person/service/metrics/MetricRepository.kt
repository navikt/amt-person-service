package no.nav.amt.person.service.metrics

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class MetricRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	fun getCounts(): Counts {
		val sql =
			"""
			select (select count(*) from person) as antall_personer,
				   (select count(*) from nav_bruker) as antall_nav_brukere,
				   (select count(*) from nav_ansatt) as antall_nav_ansatte,
				   (select count(*) from nav_enhet) as antall_nav_enheter,
				   (select count(*) from person_rolle where type = 'ARRANGOR_ANSATT') as antall_arrangor_ansatte
			""".trimIndent()

		return template
			.query(sql) { rs, _ ->
				Counts(
					antallPersoner = rs.getInt("antall_personer"),
					antallNavBrukere = rs.getInt("antall_nav_brukere"),
					antallNavAnsatte = rs.getInt("antall_nav_ansatte"),
					antallNavEnheter = rs.getInt("antall_nav_enheter"),
					antallArrangorAnsatte = rs.getInt("antall_arrangor_ansatte"),
				)
			}.first()
	}

	data class Counts(
		val antallPersoner: Int,
		val antallNavBrukere: Int,
		val antallNavAnsatte: Int,
		val antallNavEnheter: Int,
		val antallArrangorAnsatte: Int,
	)
}
