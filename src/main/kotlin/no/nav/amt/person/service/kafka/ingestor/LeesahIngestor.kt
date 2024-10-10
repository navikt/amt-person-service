package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.config.SecureLog
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.navn.Navn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class  OpplysningsType {
	NAVN_V1,
	ADRESSEBESKYTTELSE_V1,
	KONTAKTADRESSE_V1,
	BOSTEDSADRESSE_V1
}

@Service
class LeesahIngestor(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(personhendelse: Personhendelse) {
		SecureLog.secureLog.info("Personhendelse for person ${personhendelse.personidenter}\n" +
			"Opplysning: ${personhendelse.opplysningstype}\n" +
			"Endringstype: ${personhendelse.endringstype}\n " +
			"tidligereHendelseId: ${personhendelse.tidligereHendelseId}\n " +
			"HendelseId: ${personhendelse.hendelseId}\n" +
			"Opprettet: ${personhendelse.opprettet}\n" +
			"Master: ${personhendelse.master}\n" +
			"Adressebeskyttelse: ${personhendelse.adressebeskyttelse}")
		when (personhendelse.opplysningstype) {
			OpplysningsType.NAVN_V1.toString() -> handterNavn(personhendelse.personidenter, personhendelse.navn, personhendelse)
			OpplysningsType.ADRESSEBESKYTTELSE_V1.toString() ->
				handterAdressebeskyttelse(personhendelse.personidenter, personhendelse.adressebeskyttelse)
			OpplysningsType.BOSTEDSADRESSE_V1.toString() -> handterAdresse(personhendelse.personidenter)
			OpplysningsType.KONTAKTADRESSE_V1.toString() -> handterAdresse(personhendelse.personidenter)
		}
	}

	private fun handterAdressebeskyttelse(personidenter: List<String>, adressebeskyttelse: Adressebeskyttelse?) {
		if (adressebeskyttelse == null) {
			log.warn("Mottok melding med opplysningstype Adressebeskyttelse fra pdl-leesah men adressebeskyttelse manglet")
			return
		}

		val lagredePersonidenter = personService.hentPersoner(personidenter).map { it.personident }

		if (lagredePersonidenter.isEmpty()) return

		personidenter.forEach {
			navBrukerService.oppdaterAdressebeskyttelse(it)
		}
	}

	private fun handterNavn(personidenter: List<String>, navn: Navn?, hendelse: Personhendelse) {
		if (navn == null) {
			log.warn("Mottok melding med opplysningstype Navn fra pdl-leesah men navn manglet")
			SecureLog.secureLog.info("Mottok melding med opplysningstype Navn fra pdl-leesah men navn manglet for ${personidenter.joinToString { it }}")
			return
		}

		val personer = personService.hentPersoner(personidenter)

		if (personer.isEmpty()) return

		personer.forEach { person ->
			log.info("Oppdaterer navn for person ${person.id}\n" +
				"Endringstype: ${hendelse.endringstype}\n " +
				"tidligereHendelseId: ${hendelse.tidligereHendelseId}\n " +
				"HendelseId: ${hendelse.hendelseId}\n" +
				"Opprettet: ${hendelse.opprettet}\n" +
				"Master: ${hendelse.master}\n" +
				"Adressebeskyttelse: ${hendelse.adressebeskyttelse}")

			personService.upsert(person.copy(
				fornavn = navn.fornavn,
				mellomnavn = navn.mellomnavn,
				etternavn = navn.etternavn,
			))
		}
	}

	private fun handterAdresse(personidenter: List<String>) {
		val lagredePersonidenter = personService.hentPersoner(personidenter).map { it.personident }

		if (lagredePersonidenter.isEmpty()) return

		navBrukerService.oppdaterAdresse(lagredePersonidenter)
	}
}
