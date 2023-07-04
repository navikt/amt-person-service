package no.nav.amt.person.service.person.model

data class Personident(
	val ident: String,
	val historisk: Boolean,
	val type: IdentType,
)

fun finnGjeldendeIdent(identer: List<Personident>): Result<Personident> {
	return try {
	    Result.success(identer.first { !it.historisk && it.type == IdentType.FOLKEREGISTERIDENT})
	} catch (e: NoSuchElementException) {
		Result.failure(NoSuchElementException("Ingen gjeldende folkeregisterident finnes"))
	}
}

