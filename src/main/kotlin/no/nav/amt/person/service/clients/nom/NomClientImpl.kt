package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class NomClientImpl(
	private val url: String,
	private val tokenSupplier : Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
): NomClient {

	companion object {
		private val mediaTypeJson = "application/json".toMediaType()

		private val log = LoggerFactory.getLogger(NomClientImpl::class.java)
	}

	override fun hentNavAnsatt(navIdent: String): NomNavAnsatt? {
		return hentNavAnsatte(listOf(navIdent))
			.firstOrNull()
			.also { if(it == null) log.info("Fant ikke veileder i NOM med ident $navIdent") }
	}

	override fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt> {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				NomQueries.HentRessurser.query,
				NomQueries.HentRessurser.Variables(navIdenter)
			)
		)

		val request: Request = Request.Builder()
			.url("$url/graphql")
			.header("Accept", mediaTypeJson.toString())
			.header("Authorization", "Bearer ${tokenSupplier.get()}")
			.post(requestBody.toRequestBody(mediaTypeJson))
			.build()

		httpClient.newCall(request).execute().use { response ->
			response.takeUnless { it.isSuccessful }
				?.let { throw RuntimeException("Uventet status ved kall mot NOM ${it.code}") }

			response.takeUnless { response.body != null }
				?.let { throw IllegalStateException("Ingen body i response") }

			val ressurserResponse = fromJsonString<NomQueries.HentRessurser.Response>(response.body!!.string())

			return toVeiledere(ressurserResponse)
		}
	}

	private fun toVeiledere(hentIdenterResponse: NomQueries.HentRessurser.Response): List<NomNavAnsatt> {
		return hentIdenterResponse.data?.ressurser?.mapNotNull {
			if (it.code != NomQueries.HentRessurser.ResultCode.OK || it.ressurs == null) {
				log.warn("Fant ikke veileder i NOM. statusCode=${it.code}")
				return@mapNotNull null
			}

			val telefonnummer = hentTjenesteTelefonnummer(it.ressurs.telefon)

			val ansatt = it.ressurs

			NomNavAnsatt(
				navIdent = ansatt.navident,
				navn = ansatt.visningsnavn ?: "${ansatt.fornavn} ${ansatt.etternavn}",
				epost = it.ressurs.epost,
				telefonnummer = telefonnummer,
				orgTilknytning = ansatt.orgTilknytning,
			)
		} ?: emptyList()
	}

	private fun hentTjenesteTelefonnummer(telefonnumere: List<NomQueries.HentRessurser.Telefon>): String? {
		return telefonnumere.find { it.type == "NAV_KONTOR_TELEFON" }?.nummer
			?: telefonnumere.find { it.type == "NAV_TJENESTE_TELEFON" }?.nummer
	}
}
