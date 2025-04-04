package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.utils.GraphqlUtils


object PdlQueries {

	data class PdlError (
		override val message: String? = null,
		override val locations: List<GraphqlUtils.GraphqlErrorLocation>? = null,
		override val path: List<String>? = null,
		override val extensions: PdlErrorExtension? = null,
	): GraphqlUtils.GraphqlError<PdlErrorExtension>

	data class PdlErrorExtension(
		val code: String? = null,
		val classification: String? = null,
		val details: PdlErrorDetails? = null
	)

	data class PdlErrorDetails(
		val type: String? = null,
		val cause: String? = null,
		val policy: String? = null
	)

	data class PdlWarning(
		val query: String?,
		val id: String,
		val message: String,
		val details: Any?
	)

	data class Extensions(
		val warnings: List<PdlWarning>
	)

	object HentPerson {
		val query = """
			query(${"$"}ident: ID!) {
			  hentPerson(ident: ${"$"}ident) {
				navn(historikk: false) {
				  fornavn
				  mellomnavn
				  etternavn
				}
				telefonnummer {
				  landskode
				  nummer
				  prioritet
				}
				adressebeskyttelse(historikk: false) {
				  gradering
				},
				bostedsadresse(historikk: false) {
				  coAdressenavn
				  vegadresse {
					husnummer
					husbokstav
					adressenavn
					tilleggsnavn
					postnummer
				  }
				  matrikkeladresse {
				 	tilleggsnavn
				 	postnummer
				  }
				}
				oppholdsadresse(historikk: false) {
				  coAdressenavn
				  vegadresse {
				  	husnummer
				  	husbokstav
				  	adressenavn
				  	tilleggsnavn
				  	postnummer
				  }
				  matrikkeladresse {
				  	tilleggsnavn
				  	postnummer
				  }
				}
				kontaktadresse(historikk: false) {
				  coAdressenavn
				  vegadresse {
					husnummer
					husbokstav
					adressenavn
					tilleggsnavn
					postnummer
				  }
				  postboksadresse {
				 	postboks
				 	postnummer
				  }
				}
			  },
			  hentIdenter(ident: ${"$"}ident, grupper: [FOLKEREGISTERIDENT, AKTORID, NPID], historikk:true) {
			  	identer {
			  		ident,
			  		historisk,
			  		gruppe
			  	}
		      },
			}
		""".trimIndent()

		data class Response(
			override val errors: List<PdlError>?,
			override val data: ResponseData?,
			val extensions: Extensions?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, PdlErrorExtension>

		data class ResponseData(
			val hentPerson: HentPerson,
			val hentIdenter: HentIdenter,
		)

		data class HentPerson(
			val navn: List<Attribute.Navn>,
			val telefonnummer: List<Attribute.Telefonnummer>,
			val adressebeskyttelse: List<Attribute.Adressebeskyttelse>,
			val bostedsadresse: List<Attribute.Bostedsadresse>,
			val oppholdsadresse: List<Attribute.Oppholdsadresse>,
			val kontaktadresse: List<Attribute.Kontaktadresse>
		)

		data class HentIdenter(
			val identer: List<Attribute.Ident>,
		)

	}

	object HentPersonFodselsar {
		val query = """
			query(${"$"}ident: ID!) {
			  hentPerson(ident: ${"$"}ident) {
				foedselsdato {
				  foedselsaar
				}
			  },
			}
		""".trimIndent()

		data class Response(
			override val errors: List<PdlError>?,
			override val data: ResponseData?,
			val extensions: Extensions?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, PdlErrorExtension>

		data class ResponseData(
			val hentPersonFoedselsdato: HentPersonFoedselsdato,
		)

		data class HentPersonFoedselsdato(
			val foedselsdato: List<Attribute.Foedselsdato>,
		)
	}

	object HentAdressebeskyttelse {
		val query = """
			query(${"$"}ident: ID!) {
			  hentPerson(ident: ${"$"}ident) {
				adressebeskyttelse(historikk: false) {
				  gradering
				}
			  }
			}
		""".trimIndent()

		data class Response(
			override val errors: List<PdlError>?,
			override val data: ResponseData?,
			val extensions: Extensions?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, PdlErrorExtension>

		data class ResponseData(
			val hentPerson: HentPerson,
		)

		data class HentPerson(
			val adressebeskyttelse: List<Attribute.Adressebeskyttelse>
		)

	}

	object HentTelefon {
		val query = """
			query(${"$"}ident: ID!) {
			  hentPerson(ident: ${"$"}ident) {
				telefonnummer {
				  landskode
				  nummer
				  prioritet
				}
			  },
			}
		""".trimIndent()

		data class Response(
			override val errors: List<PdlError>?,
			override val data: ResponseData?,
			val extensions: Extensions?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, PdlErrorExtension>

		data class ResponseData(
			val hentPerson: HentPerson
		)

		data class HentPerson(
			val telefonnummer: List<Attribute.Telefonnummer>
		)

	}

	object HentIdenter {
		val query = """
			query(${"$"}ident: ID!) {
			  hentIdenter(ident: ${"$"}ident, grupper: [FOLKEREGISTERIDENT, AKTORID, NPID], historikk:true) {
			  	identer {
			  		ident,
			  		historisk,
			  		gruppe
			  	}
		      }
			}
		""".trimIndent()

		data class Response(
			override val errors: List<PdlError>?,
			override val data: ResponseData?,
			val extensions: Extensions?,
		) : GraphqlUtils.GraphqlResponse<ResponseData, PdlErrorExtension>

		data class ResponseData(
			val hentIdenter: HentIdenter?,
		)
		data class HentIdenter(
			val identer: List<Attribute.Ident>,
		)

	}

	object Attribute {
		data class Adressebeskyttelse(
			val gradering: String
		)

		data class Navn(
			val fornavn: String,
			val mellomnavn: String?,
			val etternavn: String,
		)

		data class Foedselsdato(
			val foedselsaar: Int
		)

		data class Ident(
			val ident: String,
			val historisk: Boolean,
			val gruppe: String,
		)
		data class Telefonnummer(
			val landskode: String,
			val nummer: String,
			val prioritet: Int,
		)

		data class Bostedsadresse(
			val coAdressenavn: String?,
			val vegadresse: Vegadresse?,
			val matrikkeladresse: Matrikkeladresse?
		)

		data class Oppholdsadresse(
			val coAdressenavn: String?,
			val vegadresse: Vegadresse?,
			val matrikkeladresse: Matrikkeladresse?
		)

		data class Kontaktadresse(
			val coAdressenavn: String?,
			val vegadresse: Vegadresse?,
			val postboksadresse: Postboksadresse?
		)

		data class Vegadresse(
			val husnummer: String?,
			val husbokstav: String?,
			val adressenavn: String?,
			val tilleggsnavn: String?,
			val postnummer: String?
		)

		data class Matrikkeladresse(
			val tilleggsnavn: String?,
			val postnummer: String?
		)

		data class Postboksadresse(
			val postboks: String,
			val postnummer: String?
		)
	}

	data class Variables(
		val ident: String,
	)

}
