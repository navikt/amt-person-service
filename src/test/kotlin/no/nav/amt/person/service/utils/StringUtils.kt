package no.nav.amt.person.service.utils

object StringUtils {
	fun nullableStringJsonValue(str: String?): String =
		if (str == null) {
			"null"
		} else {
			"\"$str\""
		}
}
