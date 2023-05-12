package no.nav.amt.person.service.clients.krr

import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.function.Supplier

class KrrProxyClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {

	fun hentKontaktinformasjon(personIdent: String): Result<Kontaktinformasjon> {
		val request: Request = Request.Builder()
			.url("$baseUrl/rest/v1/person?inkluderSikkerDigitalPost=false")
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
			.header("Nav-Personident", personIdent)
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				return Result.failure(RuntimeException("Klarte ikke å hente kontaktinformasjon fra KRR-proxy. Status: ${response.code}"))
			}

			val body = response.body?.string() ?: return Result.failure(RuntimeException("Body i respons fra KRR-proxy"))

			val responseDto = fromJsonString<KontaktinformasjonDto>(body)

			return Result.success(Kontaktinformasjon(
				epost = responseDto.epostadresse,
				telefonnummer = responseDto.mobiltelefonnummer,
			))
		}
	}

	private data class KontaktinformasjonDto(
		val epostadresse: String?,
		val mobiltelefonnummer: String?,
	)
}
