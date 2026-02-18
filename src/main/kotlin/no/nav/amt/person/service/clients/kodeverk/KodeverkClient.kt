package no.nav.amt.person.service.clients.kodeverk

import no.nav.amt.person.service.clients.HeaderConstants.NAV_CALL_ID_HEADER
import no.nav.amt.person.service.clients.HeaderConstants.NAV_CONSUMER_ID_HEADER
import no.nav.amt.person.service.poststed.Postnummer
import no.nav.common.token_client.client.MachineToMachineTokenClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

@Service
class KodeverkClient(
	@Value($$"${kodeverk.url}") private val url: String,
	@Value($$"${kodeverk.scope}") private val scope: String,
	private val kodeverkHttpClient: OkHttpClient,
	private val machineToMachineTokenClient: MachineToMachineTokenClient,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Retryable
	fun hentKodeverk(callId: UUID): List<Postnummer> {
		val request =
			Request
				.Builder()
				.url("$url/api/v1/kodeverk/Postnummer/koder/betydninger?ekskluderUgyldige=true&oppslagsdato=${LocalDate.now()}&spraak=nb")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.header(NAV_CALL_ID_HEADER, callId.toString())
				.header(NAV_CONSUMER_ID_HEADER, "amt-person-service")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
				.get()
				.build()

		try {
			kodeverkHttpClient.newCall(request).execute().use { response ->
				response
					.takeIf { !it.isSuccessful }
					?.let { throw RuntimeException("Uventet status ved kall mot kodeverk ${it.code}") }

				val kodeverkRespons = objectMapper.readValue<GetKodeverkKoderBetydningerResponse>(response.body.string())
				return kodeverkRespons.toPostnummerListe()
			}
		} catch (e: Exception) {
			log.error("Noe gikk galt ved henting av postnummer fra kodeverk: ${e.message}", e)
			throw RuntimeException("Noe gikk galt ved henting av postnummer fra kodeverk")
		}
	}
}
