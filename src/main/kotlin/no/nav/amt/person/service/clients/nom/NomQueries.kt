package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.utils.GraphqlUtils
import tools.jackson.databind.JsonNode
import java.time.LocalDate

object NomQueries {
	object HentRessurser {
		val query =
			$$"""
			query($identer: [String!]!) {
				ressurser(where: { navidenter: $identer }){
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
						primaryTelefon
					    orgTilknytning {
					  		gyldigFom
					  		gyldigTom
					  		orgEnhet {
					  			remedyEnhetId
					  		}
					  		erDagligOppfolging
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
			override val data: ResponseData?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, JsonNode>

		data class ResponseData(
			val ressurser: List<RessursResult>,
		)

		data class NomError(
			override val message: String? = null,
			override val locations: List<GraphqlUtils.GraphqlErrorLocation>? = null,
			override val path: List<String>? = null,
			override val extensions: JsonNode? = null,
		) : GraphqlUtils.GraphqlError<JsonNode>

		enum class ResultCode {
			OK,
			NOT_FOUND,
			ERROR,
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
			// Udokumentert verdi: Det eneste Nom er master for av telefoner er det ansatte selv velger å legge inn på primaryTelefon feltet,
			// men det er ikke obligatorisk og ikke primærnummer, med mindre de er eksterne
			val primaryTelefon: String?,
			val orgTilknytning: List<OrgTilknytning>,
		)

		data class OrgTilknytning(
			val gyldigFom: LocalDate,
			val gyldigTom: LocalDate?,
			val orgEnhet: OrgEnhet,
			val erDagligOppfolging: Boolean,
		) {
			data class OrgEnhet(
				val remedyEnhetId: String?,
			)
		}

		data class Telefon(
			val nummer: String,
			val type: String, // Enten NAV_TJENESTE_TELEFON eller PRIVAT_TELEFON
		)
	}
}
