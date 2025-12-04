package no.nav.amt.person.service.navbruker

import no.nav.amt.person.service.api.dto.NavBrukerFodselsdatoDto
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KontaktinformasjonForPersoner
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.kafka.consumer.LeesahConsumer.Companion.personerMedFalskIdentitet
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.navansatt.NavAnsatt
import no.nav.amt.person.service.navansatt.NavAnsattService
import no.nav.amt.person.service.navenhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.PersonUpdateEvent
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.EnvUtils
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class NavBrukerService(
	private val repository: NavBrukerRepository,
	private val personService: PersonService,
	private val navAnsattService: NavAnsattService,
	private val navEnhetService: NavEnhetService,
	private val rolleService: RolleService,
	private val krrProxyClient: KrrProxyClient,
	private val poaoTilgangClient: PoaoTilgangClient,
	private val pdlClient: PdlClient,
	private val veilarboppfolgingClient: VeilarboppfolgingClient,
	private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
	private val kafkaProducerService: KafkaProducerService,
	private val transactionTemplate: TransactionTemplate,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getNavBrukere(
		offset: Int,
		limit: Int,
	): List<NavBruker> = repository.getAllNavBrukere(offset, limit).map { it.toModel() }

	fun getNavBrukereModifiedBefore(
		limit: Int,
		modifiedBefore: LocalDate,
		lastId: UUID?,
	): List<NavBruker> =
		repository.getAllNavBrukere(limit, modifiedBefore, lastId).map {
			it.toModel()
		}

	fun getPersonidenter(
		offset: Int,
		limit: Int,
		notSyncedSince: LocalDateTime? = null,
	): List<String> = repository.getPersonidenter(offset, limit, notSyncedSince).distinct()

	fun getPersonidenterMedManglendeKontaktinfo(
		sistePersonident: String?,
		limit: Int,
	) = repository.getPersonidenterMedManglendeKontaktinfo(sistePersonident, limit)

	fun hentNavBruker(id: UUID): NavBruker = repository.get(id).toModel()

	fun hentNavBruker(personident: String): NavBruker? = repository.get(personident)?.toModel()

	fun hentEllerOpprettNavBruker(personident: String): NavBruker {
		val navBruker =
			repository.get(personident)?.toModel()?.let {
				if (it.innsatsgruppe == null) {
					oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
					repository.get(it.id).toModel()
				} else {
					it
				}
			} ?: opprettNavBruker(personident)

		return navBruker
	}

	fun hentNavBrukerFodselsar(personident: String): NavBrukerFodselsdatoDto {
		val fodselsar = pdlClient.hentPersonFodselsar(personident)
		return NavBrukerFodselsdatoDto(fodselsar)
	}

	private fun opprettNavBruker(personident: String): NavBruker {
		val personOpplysninger = pdlClient.hentPerson(personident)

		val person = personService.hentEllerOpprettPerson(personident, personOpplysninger)
		val veileder = navAnsattService.hentBrukersVeileder(personident)
		val navEnhet = navEnhetService.hentNavEnhetForBruker(personident)
		val kontaktinformasjon =
			krrProxyClient.hentKontaktinformasjon(personident).getOrElse {
				null.also { log.warn("Navbruker ${person.id} mangler kontaktinformasjon fra KRR") }
			}
		val erSkjermet = poaoTilgangClient.erSkjermetPerson(personident).getOrThrow()
		val oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingperioder(personident)
		val innsatsgruppe = veilarbvedtaksstotteClient.hentInnsatsgruppe(personident)

		val navBruker =
			NavBruker(
				id = UUID.randomUUID(),
				person = person,
				navVeileder = veileder,
				navEnhet = navEnhet,
				telefon = kontaktinformasjon?.telefonnummer ?: personOpplysninger.telefonnummer,
				epost = kontaktinformasjon?.epost,
				erSkjermet = erSkjermet,
				adresse = getAdresse(personOpplysninger),
				sisteKrrSync = LocalDateTime.now(),
				adressebeskyttelse = personOpplysninger.getAdressebeskyttelse(),
				oppfolgingsperioder = oppfolgingsperioder,
				innsatsgruppe = innsatsgruppe,
			)

		upsert(navBruker)
		log.info("Opprettet ny nav bruker med id: ${navBruker.id}")

		return repository.getByPersonId(person.id)?.toModel()
			?: throw IllegalStateException(
				"Fant ikke nav-bruker for person ${person.id}, skulle ha opprettet bruker ${navBruker.id}",
			)
	}

	fun upsert(navBruker: NavBruker) {
		transactionTemplate.executeWithoutResult {
			repository.upsert(navBruker.toUpsert())
			rolleService.opprettRolle(navBruker.person.id, Rolle.NAV_BRUKER)
			kafkaProducerService.publiserNavBruker(navBruker)
		}
	}

	fun oppdaterNavEnhet(
		navBruker: NavBruker,
		navEnhetId: UUID?,
	) {
		transactionTemplate.executeWithoutResult {
			repository.updateNavEnhet(navBruker.id, navEnhetId)
			rolleService.opprettRolle(navBruker.person.id, Rolle.NAV_BRUKER)
			// publiser oppdatert Nav-bruker
			kafkaProducerService.publiserNavBruker(repository.get(navBruker.id).toModel())
		}
	}

	fun finnBrukerId(gjeldendeIdent: String): UUID? = repository.finnBrukerId(gjeldendeIdent)

	fun oppdaterNavVeileder(
		navBrukerId: UUID,
		veileder: NavAnsatt,
	) {
		val bruker = repository.get(navBrukerId).toModel()
		if (bruker.navVeileder?.id != veileder.id) {
			upsert(bruker.copy(navVeileder = veileder))
		}
	}

	fun oppdaterOppfolgingsperiodeOgInnsatsgruppe(
		navBrukerId: UUID,
		oppfolgingsperiode: Oppfolgingsperiode,
	) {
		val bruker = repository.get(navBrukerId).toModel()
		val oppfolgingsperioder =
			bruker.oppfolgingsperioder
				.filter { it.id != oppfolgingsperiode.id }
				.plus(oppfolgingsperiode)

		if (oppfolgingsperioder.toSet() != bruker.oppfolgingsperioder.toSet()) {
			upsert(
				bruker.copy(
					oppfolgingsperioder = oppfolgingsperioder,
					innsatsgruppe =
						veilarbvedtaksstotteClient.hentInnsatsgruppe(
							bruker.person.personident,
						),
				),
			)
		}
	}

	fun oppdaterInnsatsgruppe(
		navBrukerId: UUID,
		innsatsgruppe: InnsatsgruppeV1,
	) {
		val bruker = repository.get(navBrukerId).toModel()

		if (innsatsgruppe != bruker.innsatsgruppe) {
			if (harAktivOppfolgingsperiode(bruker.oppfolgingsperioder)) {
				upsert(bruker.copy(innsatsgruppe = innsatsgruppe))
			} else if (bruker.innsatsgruppe != null) {
				upsert(bruker.copy(innsatsgruppe = null))
			}
		}
	}

	fun oppdaterOppfolgingsperiodeOgInnsatsgruppe(navBruker: NavBruker) {
		val oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingperioder(navBruker.person.personident)
		val innsatsgruppe = veilarbvedtaksstotteClient.hentInnsatsgruppe(navBruker.person.personident)

		if (navBruker.innsatsgruppe != innsatsgruppe || navBruker.oppfolgingsperioder != oppfolgingsperioder) {
			upsert(
				navBruker.copy(
					oppfolgingsperioder = oppfolgingsperioder,
					innsatsgruppe = innsatsgruppe,
				),
			)
			log.info("Oppdatert innsatsgruppe og oppfølgingsperidoe for navbruker med id ${navBruker.id}")
		}
	}

	fun oppdaterKontaktinformasjon(bruker: NavBruker) {
		val kontaktinformasjon =
			krrProxyClient.hentKontaktinformasjon(bruker.person.personident).getOrElse {
				val feilmelding =
					"Klarte ikke hente kontaktinformasjon fra KRR-Proxy for bruker ${bruker.id}: ${it.message}"
				if (EnvUtils.isDev()) {
					log.info(feilmelding)
				} else {
					log.error(feilmelding)
				}
				return
			}
		val telefon = kontaktinformasjon.telefonnummer ?: pdlClient.hentTelefon(bruker.person.personident)
		oppdaterKontaktinfo(bruker, kontaktinformasjon.copy(telefonnummer = telefon))
	}

	fun settSkjermet(
		brukerId: UUID,
		erSkjermet: Boolean,
	) {
		val bruker = repository.get(brukerId).toModel()
		if (bruker.erSkjermet != erSkjermet) {
			upsert(bruker.copy(erSkjermet = erSkjermet))
		}
	}

	fun syncKontaktinfoBulk(personident: List<String>) {
		val personerChunks =
			personident.chunked(250) // maksgrense på 500 hos krr, vi spør på færre for å redusere timeout-problematikk

		personerChunks.forEach { personChunk ->
			val krrKontaktinfo =
				krrProxyClient.hentKontaktinformasjon(personChunk.toSet()).getOrElse {
					val feilmelding = "Klarte ikke hente kontaktinformasjon fra KRR-Proxy: ${it.message}"

					if (EnvUtils.isDev()) {
						log.info(feilmelding)
					} else {
						log.error(feilmelding)
					}
					return
				}

			krrKontaktinfo.forEach kontaktinfoPersoner@{
				val telefon = it.value.telefonnummer ?: pdlClient.hentTelefon(it.key)
				val bruker = repository.get(it.key)?.toModel() ?: return@kontaktinfoPersoner
				oppdaterKontaktinfo(bruker, it.value.copy(telefonnummer = telefon))
			}
		}
		log.info("Syncet kontaktinfo for ${personerChunks.size} personer")
	}

	fun fetchOppdatertKontaktinfo(personidenter: Set<String>): KontaktinformasjonForPersoner {
		if (personidenter.size > 500) {
			throw IllegalArgumentException("Kontaktinformasjon kan henters for maks 500 personidenter i en batch")
		}
		return krrProxyClient
			.hentKontaktinformasjon(personidenter)
			.getOrThrow()
			.mapValues { (ident, info) ->
				val bruker =
					repository.get(ident)?.toModel()
						?: throw NoSuchElementException("Kunne ikke oppdatere kontakinformasjon, bruker finnes ikke")

				val tlf = info.telefonnummer ?: pdlClient.hentTelefon(ident)

				info.copy(telefonnummer = tlf).also {
					oppdaterKontaktinfo(bruker, it)
				}
			}
	}

	fun oppdaterAdressebeskyttelse(personident: String) {
		val bruker = repository.get(personident)?.toModel() ?: return

		if (bruker.person.id.toString() in personerMedFalskIdentitet) {
			log.info("Skipper oppdaterAdressebeskyttelse for ${bruker.person.id}")
			return
		}

		val personOpplysninger =
			try {
				pdlClient.hentPerson(personident)
			} catch (e: Exception) {
				val feilmelding = "Klarte ikke hente person fra PDL ved oppdatert adressebeskyttelse: ${e.message}"

				if (EnvUtils.isDev()) {
					log.info(feilmelding)
				} else {
					log.error(feilmelding)
				}

				return
			}

		val oppdatertAdressebeskyttelse = personOpplysninger.getAdressebeskyttelse()

		if (bruker.adressebeskyttelse == oppdatertAdressebeskyttelse) return

		upsert(bruker.copy(adressebeskyttelse = oppdatertAdressebeskyttelse, adresse = getAdresse(personOpplysninger)))
	}

	fun oppdaterAdresse(personidenter: List<String>) {
		personidenter.forEach {
			oppdaterAdresse(it)
		}
	}

	private fun oppdaterAdresse(personident: String) {
		val bruker = repository.get(personident)?.toModel() ?: return

		if (bruker.person.id.toString() in personerMedFalskIdentitet) {
			log.info("Skipper oppdaterAdresse for ${bruker.person.id}")
			return
		}

		val personOpplysninger =
			try {
				pdlClient.hentPerson(personident)
			} catch (e: Exception) {
				val feilmelding = "Klarte ikke hente person fra PDL ved oppdatert adresse: ${e.message}"

				if (EnvUtils.isDev()) {
					log.info(feilmelding)
				} else {
					log.error(feilmelding)
				}

				return
			}

		val oppdatertAdresse = getAdresse(personOpplysninger)

		if (bruker.adresse == oppdatertAdresse) return

		upsert(bruker.copy(adresse = oppdatertAdresse))
		log.info("Oppdatert adresse for navbruker med personId ${bruker.person.id}")
	}

	private fun getAdresse(personopplysninger: PdlPerson): Adresse? {
		val adressebeskyttelse = personopplysninger.getAdressebeskyttelse()
		return if (adressebeskyttelse == null) {
			personopplysninger.adresse
		} else {
			null
		}
	}

	@EventListener
	fun onPersonUpdate(personUpdateEvent: PersonUpdateEvent) {
		repository.get(personUpdateEvent.person.personident)?.let {
			kafkaProducerService.publiserNavBruker(it.toModel())
		}
	}

	private fun oppdaterKontaktinfo(
		bruker: NavBruker,
		kontaktinformasjon: Kontaktinformasjon,
	) {
		if (bruker.telefon == kontaktinformasjon.telefonnummer && bruker.epost == kontaktinformasjon.epost) {
			repository.upsert(bruker.copy(sisteKrrSync = LocalDateTime.now()).toUpsert())
			log.info("Ingen endring i kontaktinfo for personId ${bruker.person.id}")
			return
		}

		if (bruker.telefon != null && kontaktinformasjon.telefonnummer == null) {
			log.info("Fjerner telefonnummer for personId ${bruker.person.id}")
		}
		if (bruker.epost != null && kontaktinformasjon.epost == null) {
			log.info("Fjerner epostadresse for personId ${bruker.person.id}")
		}
		upsert(
			bruker.copy(
				telefon = kontaktinformasjon.telefonnummer,
				epost = kontaktinformasjon.epost,
				sisteKrrSync = LocalDateTime.now(),
			),
		)
	}
}
