package no.nav.amt.person.service.person.model

import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import org.postgresql.util.PGobject

data class Adresse(
	val bostedsadresse: Bostedsadresse?,
	val oppholdsadresse: Oppholdsadresse?,
	val kontaktadresse: Kontaktadresse?
) {
	fun toPGObject() = PGobject().also {
		it.type = "json"
		it.value = objectMapper.writeValueAsString(this)
	}
}

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
	val postnummer: String,
	val poststed: String
)

data class Matrikkeladresse(
	val tilleggsnavn: String?,
	val postnummer: String,
	val poststed: String
)

data class Postboksadresse(
	val postboks: String,
	val postnummer: String,
	val poststed: String
)
