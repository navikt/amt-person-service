package no.nav.amt.person.service.clients.nom

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.person.service.utils.GraphqlUtils


object NomQueries {

	object HentIdenter {
		val query = """
			query(${"$"}identer: [String!]!) {
				ressurser(where: { navidenter: ${"$"}identer }){
					code
					ressurs {
						navident
						visningsnavn
						fornavn
						etternavn
						epost
						telefon {
							nummer
							type
						}
					}
				}
			}
		""".trimIndent()

		data class Variables(
			val identer: List<String>,
		)

		data class Response(
			override val errors: List<NomError>?,
			override val data: ResponseData?
		) : GraphqlUtils.GraphqlResponse<ResponseData, JsonNode>

		data class ResponseData(
			val ressurser: List<RessursResult>,
		)

		data class NomError (
			override val message: String? = null,
			override val locations: List<GraphqlUtils.GraphqlErrorLocation>? = null,
			override val path: List<String>? = null,
			override val extensions: JsonNode? = null,
		): GraphqlUtils.GraphqlError<JsonNode>

		enum class ResultCode {
			OK,
			NOT_FOUND,
			ERROR
		}

		data class RessursResult(
			val code: ResultCode,
			val ressurs: Ressurs?,
		)

		data class Ressurs(
			val navident: String,
			val visningsnavn: String?,
			val fornavn: String?,
			val etternavn: String?,
			val epost: String?,
			val telefon: List<Telefon>,
		)

		data class Telefon(
			val nummer: String,
			val type: String // Enten NAV_TJENESTE_TELEFON eller PRIVAT_TELEFON
		)
	}

}
