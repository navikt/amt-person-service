package no.nav.amt.person.service.clients.oppfolgingskontor

import org.springframework.stereotype.Service

@Service
class OppfolgingskontorClient(
    private val api: OppfolgingskontorApi,
) {
    companion object {
        private val kontorForBrukerQuery =
            $$"""
            query HentKontorer($ident: String!) {
              kontorTilhorigheter(ident: $ident) {
                arbeidsoppfolging {
                    kontorId   
                    kontorNavn 
                }
              }
            }
            """.trimIndent()
    }

    fun hentKontorForBruker(ident: String): Arbeidsoppfolging? {
        val gqlResponse = api.hentKontorForBruker(
            OppfolgingskontorApi.GraphQLRequest(
                query = kontorForBrukerQuery,
                variables = mapOf("ident" to ident),
            ),
        )

        gqlResponse.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
            val melding = errors.joinToString(separator = "\n") { "- ${it.message}" }
            throw RuntimeException("Feilmeldinger i respons fra ao-oppfolgingskontor:\n$melding")
        }

        if (gqlResponse.data == null) {
            throw RuntimeException("ao-oppfolgingskontor respons inneholder ikke data")
        }

        return gqlResponse.data.kontorTilhorigheter.arbeidsoppfolging
    }
}
