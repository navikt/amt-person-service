package no.nav.amt.person.service.clients.pdl

data class PdlPersonIdent(
	val ident: String,
	val historisk: Boolean,
	val gruppe: String
)
