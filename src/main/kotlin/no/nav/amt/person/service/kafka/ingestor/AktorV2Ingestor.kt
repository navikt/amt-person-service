package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.IdentType
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AktorV2Ingestor(
	private val personService: PersonService
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(key: String, value: Aktor?) {
		if (value == null) {
			secureLog.warn("Fikk tombstone for record med key=$key.")
			log.warn("Fikk tombstone for kafka record. Se secure logs for key. Behandler ikke meldingen.")
			return
		}

		val identer = value.identifikatorer.filter { it.type != Type.AKTORID }

		val gjeldendeIdenter = identer.filter { it.gjeldende }.sortedWith(compareBy { it.type })

		if (gjeldendeIdenter.isEmpty()) {
			secureLog.error("AktorV2 ingestor mottok bruker med 0 gjeldende personident(er): ${value.identifikatorer}")
			log.error("AktorV2 ingestor mottok bruker med 0 gjeldende ident(er). Se secure logs for detaljer")
			throw IllegalStateException("Kan ikke ingeste bruker med 0 gjeldende ident(er)")
		}

		if (gjeldendeIdenter.size > 1) {
			secureLog.warn("AktorV2 ingestor mottok bruker med ${gjeldendeIdenter.size} personident(er): ${value.identifikatorer}")
			log.error("AktorV2 ingestor mottok bruker med ${gjeldendeIdenter.size}  gjeldende ident(er). Se secure logs for detaljer")
		}

		val gjeldendeIdent = gjeldendeIdenter.first()

		personService.oppdaterPersonIdent(
			gjeldendeIdent.idnummer,
			gjeldendeIdent.type.toModel(),
			identer.filter { it.idnummer != gjeldendeIdent.idnummer }.map { it.idnummer }
		)

	}
}

private fun Type.toModel(): IdentType {
	return when(this) {
		Type.FOLKEREGISTERIDENT -> IdentType.FOLKEREGISTERIDENT
		Type.NPID -> IdentType.NPID
		else -> throw IllegalStateException("Kan ikke håndtere ident av type $this")
	}
}
