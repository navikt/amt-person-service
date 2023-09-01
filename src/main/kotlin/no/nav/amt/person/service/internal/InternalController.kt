package no.nav.amt.person.service.internal

import jakarta.servlet.http.HttpServletRequest
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

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
		@RequestParam(value = "offset", required = false) offset: Int?
	) {
		if (isInternal(servlet)) {
			JobRunner.runAsync("oppdater-adr-republiser-nav-brukere") {
				oppdaterAdresseOgRepubliserAlleNavBrukere(startOffset = offset ?: 0)
			}
		} else {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
	}

	@Unprotected
	@GetMapping("/nav-brukere/republiser/{dollyIdent}")
	fun republiserNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("dollyIdent") dollyIdent: String
	) {
		if (isDev() && isInternal(servlet)) {
			republiserNavBruker(dollyIdent)
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
			data = navBrukerService.get(currentOffset, batchSize)
			data.forEach { action(it) }
			totalHandled += data.size
			currentOffset += batchSize
		} while (data.isNotEmpty())

		val duration = Duration.between(start, Instant.now())

		if (totalHandled > 0)
			log.info("Handled $totalHandled nav-bruker records in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")

	}

	private fun oppdaterAdresseOgRepubliserAlleNavBrukere(startOffset: Int = 0) {
		var offset = startOffset
		var navbrukere: List<NavBrukerDbo>

		do {
			navbrukere = navBrukerRepository.getAll(offset, 500)
			val personidenter = navbrukere.map { it.person.personident }
			navBrukerService.oppdaterAdresse(personidenter)

			log.info("Oppdaterte persondata for personer fra offset $offset til ${offset + navbrukere.size}")
			offset += navbrukere.size
		} while (navbrukere.isNotEmpty())
	}

	private fun republiserNavBruker(personident: String) {
		val bruker = navBrukerRepository.get(personident) ?: throw RuntimeException("Fant ikke bruker")
		kafkaProducerService.publiserNavBruker(bruker.toModel())
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}
}
