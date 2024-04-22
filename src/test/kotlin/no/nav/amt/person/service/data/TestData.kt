package no.nav.amt.person.service.data

import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.Adressebeskyttelse
import no.nav.amt.person.service.nav_bruker.Oppfolgingsperiode
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.dbo.PersonidentDbo
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Bostedsadresse
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Kontaktadresse
import no.nav.amt.person.service.person.model.Matrikkeladresse
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Vegadresse
import java.time.LocalDateTime
import java.util.UUID

object TestData {

	fun randomIdent(): String {
		return (10_00_19_00_00_000 .. 31_12_20_99_99_999).random().toString()
	}

	fun randomNavIdent(): String {
		return ('A'..'Z').random().toString() + (100_000 .. 999_999).random().toString()
	}

	fun randomEnhetId() = (1000..9999).random().toString()

	fun lagPerson(
		id: UUID = UUID.randomUUID(),
		personident: String = randomIdent(),
		fornavn: String = "Fornavn",
		mellomnavn: String? = null,
		etternavn: String = "Etternavn",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = PersonDbo(
			id,
			personident,
			fornavn,
			mellomnavn,
			etternavn,
			createdAt,
			modifiedAt
	)

	fun lagNavEnhet(
		id: UUID = UUID.randomUUID(),
		enhetId: String = randomEnhetId(),
		navn: String = "Nav Enhet 1",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavEnhetDbo(id, enhetId, navn, createdAt, modifiedAt)


	fun lagNavAnsatt(
		id: UUID = UUID.randomUUID(),
		navIdent: String = randomNavIdent(),
		navn: String = "Nav Ansatt",
		telefon: String = "99988777",
		epost: String = "ansatt@nav.no",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavAnsattDbo(
		id = id,
		navIdent = navIdent,
		navn = navn,
		telefon = telefon,
		epost = epost,
		createdAt = createdAt,
		modifiedAt = modifiedAt,
	)

	fun lagNavBruker(
		id: UUID = UUID.randomUUID(),
		person: PersonDbo = lagPerson(),
		navVeileder: NavAnsattDbo? = lagNavAnsatt(),
		navEnhet: NavEnhetDbo? = lagNavEnhet(),
		telefon: String? = "77788999",
		epost: String? = "nav_bruker@gmail.com",
		erSkjermet: Boolean = false,
		adresse: Adresse? = null,
		sisteKrrSync: LocalDateTime? = null,
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
		adressebeskyttelse: Adressebeskyttelse? = null,
		oppfolgingsperioder: List<Oppfolgingsperiode> = listOf(lagOppfolgingsperiode())
	) = NavBrukerDbo(id, person, navVeileder, navEnhet, telefon, epost, erSkjermet, adresse, sisteKrrSync, createdAt, modifiedAt, adressebeskyttelse, oppfolgingsperioder)

	fun lagOppfolgingsperiode(
		id: UUID = UUID.randomUUID(),
		startdato: LocalDateTime = LocalDateTime.now().minusMonths(1),
		sluttdato: LocalDateTime? = null,
	) = Oppfolgingsperiode(
		id,
		startdato,
		sluttdato,
	)

	fun lagPdlPerson(
        person: PersonDbo,
        telefon: String? = null,
        adressebeskyttelseGradering: AdressebeskyttelseGradering? = null,
        identer: List<Personident> = listOf(Personident(person.personident, false, IdentType.FOLKEREGISTERIDENT)),
		adresse: Adresse? = lagAdresse()
	) = PdlPerson(
		fornavn = person.fornavn,
		mellomnavn = person.mellomnavn,
		etternavn = person.etternavn,
		telefonnummer = telefon,
		adressebeskyttelseGradering = adressebeskyttelseGradering,
		identer = identer,
		adresse = adresse
	)

	fun lagPersonident(
		ident: String = randomIdent(),
		personId: UUID = UUID.randomUUID(),
		historisk: Boolean = false,
		type: IdentType = IdentType.FOLKEREGISTERIDENT,
		modifiedAt: LocalDateTime = LocalDateTime.now(),
		createdAt: LocalDateTime = LocalDateTime.now()
	) = PersonidentDbo(ident, personId, historisk, type, modifiedAt, createdAt)

	fun lagAdresse(): Adresse =
		Adresse(
			bostedsadresse = Bostedsadresse(
				coAdressenavn = "C/O Gutterommet",
				vegadresse = null,
				matrikkeladresse = Matrikkeladresse(
					tilleggsnavn = "Gården",
					postnummer = "0484",
					poststed = "OSLO"
				)
			),
			oppholdsadresse = null,
			kontaktadresse = Kontaktadresse(
				coAdressenavn = null,
				vegadresse = Vegadresse(
					husnummer = "1",
					husbokstav = null,
					adressenavn = "Gate",
					tilleggsnavn = null,
					postnummer = "1234",
					poststed = "MOSS"
				),
				postboksadresse = null
			)
		)
}
