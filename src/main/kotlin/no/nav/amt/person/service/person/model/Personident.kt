package no.nav.amt.person.service.person.model

data class Personident(
	val ident: String,
	val historisk: Boolean,
	val type: IdentType,
)

fun finnGjeldendeIdent(identer: List<Personident>): Result<Personident> {
	val gjeldendeIdent = identer.firstOrNull{
		!it.historisk && it.type == IdentType.FOLKEREGISTERIDENT
	} ?: identer.firstOrNull{
		!it.historisk && it.type == IdentType.NPID
	}

	return gjeldendeIdent?.let { Result.success(it) }
		?: Result.failure(NoSuchElementException("Ingen gjeldende personident finnes"))
}

