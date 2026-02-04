package no.nav.amt.person.service.utils

import tools.jackson.module.kotlin.jacksonObjectMapper

object JsonUtils {
	val staticObjectMapper = jacksonObjectMapper()
}
