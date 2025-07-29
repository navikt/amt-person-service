package no.nav.amt.person.service.navenhet

import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.veilarbarena.VeilarbarenaClient
import no.nav.amt.person.service.config.TeamLogs
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NavEnhetService(
	private val navEnhetRepository: NavEnhetRepository,
	private val norgClient: NorgClient,
	private val veilarbarenaClient: VeilarbarenaClient,
	private val kafkaProducerService: KafkaProducerService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentNavEnhetForBruker(personident: String): NavEnhet? {
		val oppfolgingsenhetId = veilarbarenaClient.hentBrukerOppfolgingsenhetId(personident) ?: return null

		return hentEllerOpprettNavEnhet(oppfolgingsenhetId)
			.also {
				if (it == null) {
					TeamLogs.warn("Bruker med personident=$personident har enhetId=$oppfolgingsenhetId som ikke finnes i norg")
				}
			}
	}

	fun hentEllerOpprettNavEnhet(enhetId: String) = navEnhetRepository.get(enhetId)?.toModel() ?: opprettEnhet(enhetId)

	fun hentNavEnhet(enhetId: String) = navEnhetRepository.get(enhetId)?.toModel()

	fun hentNavEnhet(id: UUID) = navEnhetRepository.get(id).toModel()

	private fun opprettEnhet(enhetId: String): NavEnhet? {
		val norgEnhet = norgClient.hentNavEnhet(enhetId) ?: return null

		val enhet =
			NavEnhet(
				id = UUID.randomUUID(),
				enhetId = enhetId,
				navn = norgEnhet.navn,
			)

		navEnhetRepository.insert(enhet)
		kafkaProducerService.publiserNavEnhet(enhet)

		return enhet
	}

	fun hentNavEnheter() = navEnhetRepository.getAll().map { it.toModel() }

	fun oppdaterNavEnheter(enheter: List<NavEnhet>) {
		val oppdaterteEnheter = norgClient.hentNavEnheter(enheter.map { it.enhetId }).associateBy { it.enhetId }
		enheter.forEach { opprinneligEnhet ->
			val oppdatertEnhet = oppdaterteEnheter[opprinneligEnhet.enhetId]

			if (oppdatertEnhet != null && oppdatertEnhet.navn != opprinneligEnhet.navn) {
				val enhetMedNyttNavn = opprinneligEnhet.copy(navn = oppdatertEnhet.navn)
				navEnhetRepository.update(enhetMedNyttNavn)
				kafkaProducerService.publiserNavEnhet(enhetMedNyttNavn)
				log.info("Oppdaterer navn for enhetId=${opprinneligEnhet.enhetId} fra '${opprinneligEnhet.navn}' til '${oppdatertEnhet.navn}'")
			} else if (oppdatertEnhet == null) {
				log.error("Fant ikke enhet for enhetId=${opprinneligEnhet.enhetId} i Norg")
			}
		}
	}
}
