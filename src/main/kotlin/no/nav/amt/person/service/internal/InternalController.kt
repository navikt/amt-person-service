package no.nav.amt.person.service.internal

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.person.service.controller.request.NavBrukerRequest
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_bruker.NavBrukerRepository
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.person.ArrangorAnsattService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.utils.EnvUtils.isDev
import no.nav.common.job.JobRunner
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/internal")
class InternalController(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
	private val personUpdater: PersonUpdater,
	private val navBrukerRepository: NavBrukerRepository,
	private val arrangorAnsattService: ArrangorAnsattService,
	private val navAnsattService: NavAnsattService,
	private val kafkaProducerService: KafkaProducerService
) {
	private val log = LoggerFactory.getLogger(InternalController::class.java)

	@Unprotected
	@PostMapping("/person/{dollyIdent}")
	fun opprettPerson(
		servlet: HttpServletRequest,
		@PathVariable("dollyIdent") dollyIdent: String,
	) {
		if (isDev() && isInternal(servlet)) {
			personService.hentEllerOpprettPerson(dollyIdent)
		}
	}

	@Unprotected
	@PostMapping("/nav-bruker/{dollyIdent}")
	fun opprettNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("dollyIdent") dollyIdent: String,
	) {
		if (isDev() && isInternal(servlet)) {
			navBrukerService.hentEllerOpprettNavBruker(dollyIdent)
		}
	}

	@Unprotected
	@PostMapping("/person/identer")
	fun oppdaterPersonidenter(
		servlet: HttpServletRequest,
		@RequestParam(value = "offset", required = false) offset: Int?,
	) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("oppdater_personidenter") {
				personUpdater.oppdaterPersonidenter(offset ?: 0)
			}
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/republiser")
	fun republiserNavBrukere(
		servlet: HttpServletRequest,
		@RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
		@RequestParam(value = "batchSize", required = false) batchSize: Int?) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("republiser-nav-brukere") {
				batchHandterNavBrukere(startFromOffset?:0, batchSize?:500) { kafkaProducerService.publiserNavBruker(it) }
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/oppdater-adr-republiser")
	fun oppdaterOgRepubliserNavBrukere(
		servlet: HttpServletRequest,
		@RequestParam(value = "batchSize", required = false) batchSize: Int = 500,
		@RequestParam(value = "modifiedBefore", required = false) modifiedBefore: LocalDateTime = LocalDateTime.now(),
		@RequestParam(value = "lastId", required = false) lastId: UUID? = null,
	) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("oppdater-adr-republiser-nav-brukere") {
				log.info("Oppdaterer adresse for alle navbrukere som mangler adresse")
				oppdaterAdresseHvisManglerOgRepubliser(modifiedBefore, batchSize, lastId)
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-bruker/oppdater-adr-republiser/{id}")
	fun oppdaterAdresseOgRepubliserNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("id") id: UUID,
	) {
		if (isInternal(servlet)) {
			log.info("Oppdaterer adresse for navbruker-id $id")
			val navBruker = navBrukerService.hentNavBruker(id)
			navBrukerService.oppdaterAdresse(listOf(navBruker.person.personident))
			log.info("Oppdaterte adresse for navbruker-id $id")
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/oppdater-innsats-republiser")
	fun oppdaterOppfolgingInnsatsOgRepubliserNavBrukere(
		servlet: HttpServletRequest,
		@RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
		@RequestParam(value = "batchSize", required = false) batchSize: Int = 500,
		@RequestParam(value = "modifiedBefore", required = false) modifiedBefore: LocalDate? = null,
		@RequestParam(value = "lastId", required = false) lastId: UUID? = null,
	) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("oppdater-innsats-republiser-nav-brukere") {
				if (modifiedBefore != null) {
					batchHandterNavBrukereByModifiedBefore(modifiedBefore, batchSize, lastId) {
						navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
					}
				} else {
					batchHandterNavBrukere(startFromOffset?:0, batchSize) {
						navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
					}
				}
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-bruker/oppdater-innsats-republiser/{id}")
	fun oppdaterOppfolgingInnsatsOgRepubliserNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("id") id: UUID,
	) {
		if (isInternal(servlet)) {
			log.info("Oppdaterer bruker $id")
			val navBruker = navBrukerService.hentNavBruker(id)
			navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(navBruker)
			log.info("Oppdaterte bruker $id")
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/republiser/{navBrukerId}")
	fun republiserNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("navBrukerId") navBrukerId: UUID
	) {
		if (isInternal(servlet)) {
			republiserNavBruker(navBrukerId)
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/arrangor-ansatte/republiser")
	fun republiserArrangorAnsatte(
		servlet: HttpServletRequest,
		@RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
		@RequestParam(value = "batchSize", required = false) batchSize: Int?) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("republiser-arrangor-ansatte") {
				republiserAlleArrangorAnsatte(startFromOffset?:0, batchSize?:500)
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-ansatte/republiser")
	fun republiserNavAnsatte(servlet: HttpServletRequest) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("republiser-nav-ansatte") {
				republiserAlleNavAnsatte()
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/synkroniser-krr")
	fun synkroniserKrr(
		servlet: HttpServletRequest,
		@RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
		@RequestParam(value = "batchSize", required = false) batchSize: Int?) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("synkroniser-krr-nav-brukere") {
				val offset = startFromOffset?:0
				val limit = batchSize?:5000
				val personidenter = navBrukerService.getPersonidenter(offset, limit, notSyncedSince = LocalDateTime.now().minusDays(3))
				navBrukerService.syncKontaktinfoBulk(personidenter)
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@PostMapping("/nav-brukere/synkroniser-krr")
	fun synkroniserKrrForPerson(
		servlet: HttpServletRequest,
		@RequestBody request: NavBrukerRequest
	) {
		if (isInternal(servlet)) {
			val navBruker = navBrukerService.hentNavBruker(request.personident) ?: throw IllegalArgumentException("Fant ikke person")
			navBrukerService.oppdaterKontaktinformasjon(navBruker)
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	private fun republiserAlleNavAnsatte() {
		val ansatte = navAnsattService.getAll()
		ansatte.forEach { kafkaProducerService.publiserNavAnsatt(it) }
		log.info("Publiserte ${ansatte.size} navansatte")
	}

	private fun republiserAlleArrangorAnsatte(startFromOffset: Int, batchSize: Int) {
		var offset = startFromOffset
		var ansatte: List<Person>

		do {
			ansatte = arrangorAnsattService.getAll(offset, batchSize)
			ansatte.forEach { kafkaProducerService.publiserArrangorAnsatt(it) }
			log.info("Publiserte arrangøransatte fra offset $offset til ${offset + ansatte.size}")
			offset += batchSize
		} while (ansatte.isNotEmpty())
	}

	private fun batchHandterNavBrukere(startFromOffset: Int, batchSize: Int, action: (navBruker: NavBruker) -> Unit) {
		var currentOffset = startFromOffset
		var data: List<NavBruker>

		val start = Instant.now()
		var totalHandled = 0

		do {
			data = navBrukerService.getNavBrukere(currentOffset, batchSize)
			data.forEach { action(it) }
			totalHandled += data.size
			currentOffset += batchSize
		} while (data.isNotEmpty())

		val duration = Duration.between(start, Instant.now())

		if (totalHandled > 0)
			log.info("Handled $totalHandled nav-bruker records in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")

	}

	private fun batchHandterNavBrukereByModifiedBefore(
		modifiedBefore: LocalDate,
		batchSize: Int,
		startAfterId: UUID?,
		action: (navBruker: NavBruker) -> Unit
	) {
		var lastId: UUID? = startAfterId
		var data: List<NavBruker>

		val start = Instant.now()
		var totalHandled = 0

		do {
			data = navBrukerService.getNavBrukereModifiedBefore(batchSize, modifiedBefore, lastId)
			data.forEach { action(it) }
			totalHandled += data.size
			lastId = data.lastOrNull()?.id
			log.info("Handled nav-bruker batch $totalHandled records. lastId $lastId")
		} while (data.isNotEmpty())

		val duration = Duration.between(start, Instant.now())

		if (totalHandled > 0)
			log.info("Handled $totalHandled nav-bruker records in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")

	}

	private fun oppdaterAdresseHvisManglerOgRepubliser(
		modifiedBefore: LocalDateTime,
		batchSize: Int,
		startAfterId: UUID?
	) {
		var lastId: UUID? = startAfterId
		var navbrukere: List<NavBrukerDbo>

		do {
			navbrukere = navBrukerRepository.getAllUtenAdresse(batchSize, modifiedBefore, lastId)
			val personidenter = navbrukere.map { it.person.personident }
			navBrukerService.oppdaterAdresse(personidenter)
			lastId = navbrukere.lastOrNull()?.id
			log.info("Oppdaterte adresse for ${navbrukere.size} personer. Siste navbrukerid: $lastId")
		} while (navbrukere.isNotEmpty())
	}

	private fun republiserNavBruker(navBrukerId: UUID) {
		val bruker = navBrukerRepository.get(navBrukerId)
		kafkaProducerService.publiserNavBruker(bruker.toModel())
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}
}
