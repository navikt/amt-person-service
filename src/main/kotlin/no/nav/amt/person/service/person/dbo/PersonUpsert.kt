package no.nav.amt.person.service.person.dbo

import no.nav.amt.person.service.person.model.IdentType
import java.util.*

data class PersonUpsert(
	val id: UUID,
	val personIdent: String,
	val personIdentType: IdentType?,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val historiskeIdenter: List<String>,
)
