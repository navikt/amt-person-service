package no.nav.amt.person.service.nav_ansatt

import no.nav.amt.person.service.clients.nom.NomClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NavAnsattUpdater(
	private val repository: NavAnsattRepository,
	private val nomClient: NomClient,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterAlle(batchSize: Int = 100) {
		val ansattBatcher = repository.getAll().chunked(batchSize)

		ansattBatcher.forEach { batch ->
			val ansatte = batch.associate { it.navIdent to AnsattSomSkalOppdateres(it.id, false) }
			val nomResultat = nomClient.hentNavAnsatte(ansatte.keys.toList())

			val oppdaterteAnsatte = nomResultat.map { nomAnsatt ->
				ansatte[nomAnsatt.navIdent]?.let {
					it.erOppdatert = true
					NavAnsatt(
						id = it.id,
						navIdent = nomAnsatt.navIdent,
						navn = nomAnsatt.navn,
						epost = nomAnsatt.epost,
						telefon = nomAnsatt.telefonnummer,
					)
				} ?: throw IllegalStateException("Ukjent NAVident ${nomAnsatt.navIdent} i respons fra Nom")
			}

			repository.upsertMany(oppdaterteAnsatte)

			ansatte.forEach { navIdent, ansattSomSkalOppdateres ->
				if (!ansattSomSkalOppdateres.erOppdatert) {
					log.warn("Fant ikke nav ansatt med ident=${navIdent} id=${ansattSomSkalOppdateres.id} i NOM")
				}
			}
		}
	}

	data class AnsattSomSkalOppdateres (
		val id: UUID,
		var erOppdatert: Boolean,
	)
}
