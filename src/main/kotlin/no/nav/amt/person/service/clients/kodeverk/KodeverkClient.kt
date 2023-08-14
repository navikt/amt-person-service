package no.nav.amt.person.service.clients.kodeverk

import no.nav.amt.person.service.poststed.PostInformasjon
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class KodeverkClient(
	@Value("\${kodeverk.url}") private val url: String,
	private val kodeverkHttpClient: OkHttpClient
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Retryable
	fun hentKodeverk(callId: UUID): List<PostInformasjon> {
		val request = Request.Builder()
			.url("$url/api/v1/kodeverk/Postnummer/koder/betydninger?ekskluderUgyldige=true&oppslagsdato=${LocalDate.now()}&spraak=nb")
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
			.header("Nav-Call-Id", callId.toString())
			.header("Nav-Consumer-Id", "amt-person-service")
			.get()
			.build()

		try {
			kodeverkHttpClient.newCall(request).execute().use { response ->
				response.takeIf { !it.isSuccessful }
					?.let { throw RuntimeException("Uventet status ved kall mot kodeverk ${it.code}") }

				val kodeverkRespons = response.body?.string()?.let {
					fromJsonString<GetKodeverkKoderBetydningerResponse>(it)
				} ?: throw RuntimeException("Tom respons fra kodeverk")

				return kodeverkRespons.toPostInformasjonListe()
			}
		} catch (e: Exception) {
			log.error("Noe gikk galt ved henting av postinformasjon fra kodeverk: ${e.message}", e)
			throw RuntimeException("Noe gikk galt ved henting av postinformasjon fra kodeverk")
		}
	}
}

data class GetKodeverkKoderBetydningerResponse(
	val betydninger: Map<String, List<Betydning>>
) {
	fun toPostInformasjonListe(): List<PostInformasjon> {
		return betydninger.map {
			PostInformasjon(
				postnummer = it.key,
				poststed = it.value.first().beskrivelser["nb"]?.term
					?: throw RuntimeException("Kode ${it.key} mangler term")
			)
		}
	}
}

data class Betydning(
	val beskrivelser: Map<String, Beskrivelse>
)

data class Beskrivelse(
	val term: String
)
