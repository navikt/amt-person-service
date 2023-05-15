package no.nav.amt.person.service.kafka.producer.dto

import no.nav.amt.person.service.person.model.IdentType
import java.util.*

data class NavBrukerDtoV1 (
	val id: UUID,
	val personIdent: String,
	val personIdentType: IdentType?,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhet: NavEnhetDtoV1?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
)
