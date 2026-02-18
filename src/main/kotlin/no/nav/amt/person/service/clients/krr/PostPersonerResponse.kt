package no.nav.amt.person.service.clients.krr

data class PostPersonerResponse(
	val personer: Map<String, KontaktinformasjonDto>,
	val feil: Map<String, String>,
) {
	data class KontaktinformasjonDto(
		val personident: String,
		val epostadresse: String?,
		val mobiltelefonnummer: String?,
	)
}
