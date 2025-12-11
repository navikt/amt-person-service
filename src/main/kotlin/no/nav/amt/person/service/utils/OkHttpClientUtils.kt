package no.nav.amt.person.service.utils

import okhttp3.MediaType.Companion.toMediaType
import org.springframework.http.MediaType

object OkHttpClientUtils {
	val mediaTypeJson = MediaType.APPLICATION_JSON_VALUE.toMediaType()
}
