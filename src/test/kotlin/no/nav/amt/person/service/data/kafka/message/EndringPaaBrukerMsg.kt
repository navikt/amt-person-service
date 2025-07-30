package no.nav.amt.person.service.data.kafka.message

import no.nav.amt.person.service.utils.StringUtils.nullableStringJsonValue

data class EndringPaaBrukerMsg(
	val fodlsesnummer: String,
	val oppfolgingsenhet: String?,
) {
	fun toJson(): String =
		"""
		{
			"fodselsnummer": ${this.fodlsesnummer},
			"oppfolgingsenhet": ${nullableStringJsonValue(this.oppfolgingsenhet)}
		}
		""".trimIndent()
}
