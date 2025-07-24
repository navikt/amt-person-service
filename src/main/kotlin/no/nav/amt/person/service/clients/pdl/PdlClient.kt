package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Bostedsadresse
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Kontaktadresse
import no.nav.amt.person.service.person.model.Matrikkeladresse
import no.nav.amt.person.service.person.model.Oppholdsadresse
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Postboksadresse
import no.nav.amt.person.service.person.model.Vegadresse
import no.nav.amt.person.service.poststed.Postnummer
import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlResponse
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class PdlClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
	private val poststedRepository: PoststedRepository
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val mediaTypeJson = "application/json".toMediaType()

	private val behandlingsnummer = "B446" // https://behandlingskatalog.nais.adeo.no/process/team/5345bce7-e076-4b37-8bf4-49030901a4c3/b3003849-c4bb-4c60-a4cb-e07ce6025623

	fun hentPerson(personident: String): PdlPerson {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentPerson.query,
				PdlQueries.Variables(personident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = fromJsonString<PdlQueries.HentPerson.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse) // respons kan inneholde feil selv om den ikke er tom ref: https://pdldocs-navno.msappproxy.net/ekstern/index.html#appendix-graphql-feilhandtering

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return toPdlBruker(gqlResponse.data)
		}
	}

	fun hentPersonFodselsar(personident: String): Int {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentPersonFodselsar.query,
				PdlQueries.Variables(personident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = fromJsonString<PdlQueries.HentPersonFodselsar.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse) // respons kan inneholde feil selv om den ikke er tom ref: https://pdldocs-navno.msappproxy.net/ekstern/index.html#appendix-graphql-feilhandtering

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			val fodselsdato = gqlResponse.data.hentPerson.foedselsdato.firstOrNull() ?: throw RuntimeException("PDL person mangler fodselsdato")
			return fodselsdato.foedselsaar
		}
	}

	fun hentIdenter(ident: String): List<Personident> {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentIdenter.query,
				PdlQueries.Variables(ident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = fromJsonString<PdlQueries.HentIdenter.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data?.hentIdenter == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return gqlResponse.data.hentIdenter.identer.map { Personident(it.ident, it.historisk, IdentType.valueOf(it.gruppe)) }
		}
	}

	fun hentTelefon(ident: String): String? {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentTelefon.query,
				PdlQueries.Variables(ident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = fromJsonString<PdlQueries.HentTelefon.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)
			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return getTelefonnummer(gqlResponse.data.hentPerson.telefonnummer)
		}
	}

	fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentAdressebeskyttelse.query,
				PdlQueries.Variables(personident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = fromJsonString<PdlQueries.HentAdressebeskyttelse.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)
			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return getDiskresjonskode(gqlResponse.data.hentPerson.adressebeskyttelse)
		}

	}

	private fun createGraphqlRequest(jsonPayload: String): Request {
		return Request.Builder()
			.url("$baseUrl/graphql")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.addHeader("Tema", "GEN")
			.addHeader("behandlingsnummer", behandlingsnummer)
			.post(jsonPayload.toRequestBody(mediaTypeJson))
			.build()
	}

	private fun toPdlBruker(response: PdlQueries.HentPerson.ResponseData): PdlPerson {
		val navn = response.hentPerson.navn.firstOrNull() ?: throw RuntimeException("PDL person mangler navn")
		val telefonnummer = getTelefonnummer(response.hentPerson.telefonnummer)
		val diskresjonskode = getDiskresjonskode(response.hentPerson.adressebeskyttelse)

		return PdlPerson(
			fornavn = navn.fornavn,
			mellomnavn = navn.mellomnavn,
			etternavn = navn.etternavn,
			telefonnummer = telefonnummer,
			adressebeskyttelseGradering = diskresjonskode,
			identer = response.hentIdenter.identer.map { Personident(it.ident, it.historisk, IdentType.valueOf(it.gruppe)) },
			adresse = getAdresse(response.hentPerson)
		)
	}

	private fun getTelefonnummer(telefonnummere: List<PdlQueries.Attribute.Telefonnummer>): String? {
		val prioritertNummer = telefonnummere.minByOrNull { it.prioritet } ?: return null

		return "${prioritertNummer.landskode}${prioritertNummer.nummer}"
	}

	private fun getDiskresjonskode(adressebeskyttelse: List<PdlQueries.Attribute.Adressebeskyttelse>): AdressebeskyttelseGradering? {
		return when(adressebeskyttelse.firstOrNull()?.gradering) {
			"STRENGT_FORTROLIG_UTLAND" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
			"STRENGT_FORTROLIG" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG
			"FORTROLIG" -> AdressebeskyttelseGradering.FORTROLIG
			"UGRADERT" -> AdressebeskyttelseGradering.UGRADERT
			else -> null
		}
	}

	private fun getAdresse(hentPersonResponse: PdlQueries.HentPerson.HentPerson): Adresse? {
		val kontaktadresseFraPdl = hentPersonResponse.kontaktadresse.firstOrNull()
		val bostedsadresseFraPdl = hentPersonResponse.bostedsadresse.firstOrNull()
		val oppholdsadresseFraPdl = hentPersonResponse.oppholdsadresse.firstOrNull()

		val unikePostnummer = listOfNotNull(
			kontaktadresseFraPdl?.vegadresse?.postnummer,
			kontaktadresseFraPdl?.postboksadresse?.postnummer,
			bostedsadresseFraPdl?.vegadresse?.postnummer,
			bostedsadresseFraPdl?.matrikkeladresse?.postnummer,
			oppholdsadresseFraPdl?.vegadresse?.postnummer,
			oppholdsadresseFraPdl?.matrikkeladresse?.postnummer
		).distinct()

		val poststeder = poststedRepository.getPoststeder(unikePostnummer)

		if (poststeder.isEmpty()) {
			return null
		}

		val adresse = Adresse(
			bostedsadresse = bostedsadresseFraPdl?.toBostedsadresse(poststeder),
			oppholdsadresse = oppholdsadresseFraPdl?.toOppholdsadresse(poststeder),
			kontaktadresse = kontaktadresseFraPdl?.toKontaktadresse(poststeder)
		)

		if (adresse.bostedsadresse == null && adresse.oppholdsadresse == null && adresse.kontaktadresse == null) {
			return null
		}
		return adresse
	}

	private fun PdlQueries.Attribute.Bostedsadresse.toBostedsadresse(poststeder: List<Postnummer>): Bostedsadresse? {
		if (vegadresse == null && matrikkeladresse == null) {
			return null
		}
		val bostedsadresse = Bostedsadresse(
			coAdressenavn = coAdressenavn,
			vegadresse = vegadresse?.toVegadresse(poststeder),
			matrikkeladresse = matrikkeladresse?.toMatrikkeladresse(poststeder)
		)
		if (bostedsadresse.vegadresse == null && bostedsadresse.matrikkeladresse == null) {
			return null
		}
		return bostedsadresse
	}

	private fun PdlQueries.Attribute.Oppholdsadresse.toOppholdsadresse(poststeder: List<Postnummer>): Oppholdsadresse? {
		if (vegadresse == null && matrikkeladresse == null) {
			return null
		}
		val oppholdsadresse = Oppholdsadresse(
			coAdressenavn = coAdressenavn,
			vegadresse = vegadresse?.toVegadresse(poststeder),
			matrikkeladresse = matrikkeladresse?.toMatrikkeladresse(poststeder)
		)
		if (oppholdsadresse.vegadresse == null && oppholdsadresse.matrikkeladresse == null) {
			return null
		}
		return oppholdsadresse
	}

	private fun PdlQueries.Attribute.Kontaktadresse.toKontaktadresse(poststeder: List<Postnummer>): Kontaktadresse? {
		if (vegadresse == null && postboksadresse == null) {
			return null
		}
		val kontaktadresse = Kontaktadresse(
			coAdressenavn = coAdressenavn,
			vegadresse = vegadresse?.toVegadresse(poststeder),
			postboksadresse = postboksadresse?.toPostboksadresse(poststeder)
		)
		if (kontaktadresse.vegadresse == null && kontaktadresse.postboksadresse == null) {
			return null
		}
		return kontaktadresse
	}

	private fun PdlQueries.Attribute.Vegadresse.toVegadresse(poststeder: List<Postnummer>): Vegadresse? {
		if (postnummer == null) {
			return null
		}
		val poststed = poststeder.find { it.postnummer == postnummer } ?: return null

		return Vegadresse(
			husnummer = husnummer,
			husbokstav = husbokstav,
			adressenavn = adressenavn,
			tilleggsnavn = tilleggsnavn,
			postnummer = postnummer,
			poststed = poststed.poststed
		)
	}

	private fun PdlQueries.Attribute.Matrikkeladresse.toMatrikkeladresse(poststeder: List<Postnummer>): Matrikkeladresse? {
		if (postnummer == null) {
			return null
		}
		val poststed = poststeder.find { it.postnummer == postnummer } ?: return null

		return Matrikkeladresse(
			tilleggsnavn = tilleggsnavn,
			postnummer = postnummer,
			poststed = poststed.poststed
		)
	}

	private fun PdlQueries.Attribute.Postboksadresse.toPostboksadresse(poststeder: List<Postnummer>): Postboksadresse? {
		if (postnummer == null) {
			return null
		}
		val poststed = poststeder.find { it.postnummer == postnummer } ?: return null

		return Postboksadresse(
			postboks = postboks,
			postnummer = postnummer,
			poststed = poststed.poststed
		)
	}

	private fun throwPdlApiErrors(response: GraphqlResponse<*, PdlQueries.PdlErrorExtension>) {
		var melding = "Feilmeldinger i respons fra pdl:\n"
		if(response.data == null) melding = "$melding- data i respons er null \n"
		response.errors?.let { feilmeldinger ->
			melding += feilmeldinger.joinToString(separator = "") { "- ${it.message} (code: ${it.extensions?.code} details: ${it.extensions?.details})\n" }
			throw RuntimeException(melding)
		}
	}

	private fun logPdlWarnings(warnings: List<PdlQueries.PdlWarning>?) {
		if (warnings == null) return
		val stringBuilder = StringBuilder("Respons fra Pdl inneholder warnings:\n")
		warnings.forEach {
			stringBuilder.append(
				"query: ${it.query},\n" +
					"id: ${it.id},\n" +
					"message: ${it.message},\n" +
					"details: ${it.details}\n"
			)
		}

		log.warn(stringBuilder.toString())
	}

}
