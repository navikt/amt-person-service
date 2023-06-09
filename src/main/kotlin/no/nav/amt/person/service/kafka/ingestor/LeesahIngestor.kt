package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.navn.Navn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class  OpplysningsType {
	NAVN_V1,
	ADRESSEBESKYTTELSE_V1,
}

@Service
class LeesahIngestor(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(personhendelse: Personhendelse) {
		when (personhendelse.opplysningstype) {
			OpplysningsType.NAVN_V1.toString() -> handterNavn(personhendelse.personidenter, personhendelse.navn)
			OpplysningsType.ADRESSEBESKYTTELSE_V1.toString() ->
				handterAdressebeskyttelse(personhendelse.personidenter, personhendelse.adressebeskyttelse)
		}
	}

	private fun handterAdressebeskyttelse(personidenter: List<String>, adressebeskyttelse: Adressebeskyttelse?) {
		if (adressebeskyttelse == null) {
			log.warn("Mottok melding med opplysningstype Adressebeskyttelse fra pdl-leesah men adressebeskyttelse manglet")
			return
		}

		val personer = personService.hentPersoner(personidenter)

		if (personer.isEmpty()) return

		if (erAddressebeskyttet(adressebeskyttelse.gradering)) {
			secureLog.info("Sletter addressebeskyttet personer med personidenter")
			// Sletter ikke arrangøransatte med adressebeskyttelse foreløpig, fordi vi ikke har
			// funksjonalitet for å håndtere dette i resten av systemet, og det er litt uklart om det er noe vi skal gjøre noe med.
			navBrukerService.slettBrukere(personer)
		}
	}

	private fun handterNavn(personidenter: List<String>, navn: Navn?) {
		if (navn == null) {
			log.warn("Mottok melding med opplysningstype Navn fra pdl-leesah men navn manglet")
			return
		}

		val personer = personService.hentPersoner(personidenter)

		if (personer.isEmpty()) return

		personer.forEach { person ->
			personService.upsert(person.copy(
				fornavn = navn.fornavn,
				mellomnavn = navn.mellomnavn,
				etternavn = navn.etternavn,
			))
		}
		navBrukerService.oppdaterKontaktinformasjon(personer)
	}

	private fun erAddressebeskyttet(gradering: Gradering?): Boolean {
		return gradering != null && gradering != Gradering.UGRADERT
	}
}
