package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.config.TeamLogs
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.finnGjeldendeIdent
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AktorV2Consumer(
	private val personService: PersonService
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(key: String, value: Aktor?) {
		if (value == null) {
			TeamLogs.warn("Fikk tombstone for record med key=$key.")
			log.warn("Fikk tombstone for kafka record. Se team logs for key. Behandler ikke meldingen.")
			return
		}

		val identer = value.identifikatorer.map { Personident(it.idnummer, !it.gjeldende, it.type.toModel()) }

		if (finnGjeldendeIdent(identer).isFailure) {
			TeamLogs.error("AktorV2 ingestor mottok bruker med 0 gjeldende personident(er): ${value.identifikatorer}")
			log.error("AktorV2 ingestor mottok bruker med 0 gjeldende ident(er). Se team logs for detaljer")
			throw IllegalStateException("Kan ikke ingeste bruker med 0 gjeldende ident(er)")
		}

		personService.oppdaterPersonIdent(identer)
	}
}

private fun Type.toModel(): IdentType {
	return when(this) {
		Type.FOLKEREGISTERIDENT -> IdentType.FOLKEREGISTERIDENT
		Type.NPID -> IdentType.NPID
		Type.AKTORID -> IdentType.AKTORID
	}
}
