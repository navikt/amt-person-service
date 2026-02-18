package no.nav.amt.person.service.utils

import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

object StringUtils {
	fun emptyRequest(): RequestBody = "".toRequestBody(mediaTypeJson)
}
