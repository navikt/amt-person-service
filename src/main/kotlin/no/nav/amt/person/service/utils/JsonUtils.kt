package no.nav.amt.person.service.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonUtils {

	val objectMapper: ObjectMapper = ObjectMapper()
		.registerKotlinModule()
		.registerModule(JavaTimeModule())
		.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

	inline fun <reified T : Any> fromJsonString(jsonStr: String): T = objectMapper.readValue(jsonStr)

	fun toJsonString(any: Any): String = objectMapper.writeValueAsString(any)
}
