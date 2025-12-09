package no.nav.amt.person.service.utils

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object JsonUtils {
	val staticObjectMapper: ObjectMapper =
		JsonMapper
			.builder()
			.addModule(KotlinModule.Builder().build())
			.build()
}
