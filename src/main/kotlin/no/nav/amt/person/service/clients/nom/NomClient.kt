package no.nav.amt.person.service.clients.nom

interface NomClient {
	fun hentNavAnsatt(navIdent: String): NomNavAnsatt?

	fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt>
}
