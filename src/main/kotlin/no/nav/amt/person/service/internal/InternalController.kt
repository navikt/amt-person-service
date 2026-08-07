package no.nav.amt.person.service.internal

import no.nav.amt.person.service.api.request.NavBrukerRequest
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.navansatt.NavAnsattRepository
import no.nav.amt.person.service.navansatt.NavAnsattUpdater
import no.nav.amt.person.service.navbruker.NavBrukerDbo
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetUpdateJob
import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.PersonidentRepository
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.EnvUtils.isDev
import no.nav.common.job.JobRunner
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Suppress("SpringMvcPathVariableDeclarationInspection")
@RestController
@RequestMapping("/internal")
class InternalController(
    private val personService: PersonService,
    private val personRepository: PersonRepository,
    private val navBrukerRepository: NavBrukerRepository,
    private val navBrukerService: NavBrukerService,
    private val personUpdater: PersonUpdater,
    private val navAnsattRepository: NavAnsattRepository,
    private val kafkaProducerService: KafkaProducerService,
    private val navAnsattUpdater: NavAnsattUpdater,
    private val navEnhetUpdateJob: NavEnhetUpdateJob,
    private val personidentRepository: PersonidentRepository,
) {
    private val log = LoggerFactory.getLogger(InternalController::class.java)

    @PostMapping("/person/{dollyIdent}")
    fun opprettPerson(
        @PathVariable("dollyIdent") dollyIdent: String,
    ) {
        if (isDev()) {
            personService.hentEllerOpprettPerson(dollyIdent)
        }
    }

    @PostMapping("/nav-bruker/{dollyIdent}")
    fun opprettNavBruker(
        @PathVariable("dollyIdent") dollyIdent: String,
    ) {
        if (isDev()) {
            navBrukerService.hentEllerOpprettNavBruker(dollyIdent)
        }
    }

    @PostMapping("/person/identer")
    fun oppdaterPersonidenter(
        @RequestParam(value = "offset", required = false) offset: Int?,
    ) {
        JobRunner.runAsync("oppdater_personidenter") {
            personUpdater.oppdaterPersonidenter(offset ?: 0)
        }
    }

    @GetMapping("/person/navn/{id}")
    fun oppdaterNavn(
        @PathVariable id: UUID,
    ) {
        val person = personRepository.get(id)

        if (person.erFalskIdentitet) log.warn("Person $id har erFalskIdentitet = true")

        personService.oppdaterNavn(person)
    }

    @GetMapping("/nav-brukere/republiser")
    fun republiserNavBrukere(
        @RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
        @RequestParam(value = "batchSize", required = false) batchSize: Int?,
    ) {
        JobRunner.runAsync("republiser-nav-brukere") {
            batchHandterNavBrukere(startFromOffset ?: 0, batchSize ?: 500) { navBruker ->
                kafkaProducerService.publiserNavBruker(
                    navBruker,
                )
            }
        }
    }

    @GetMapping("/nav-brukere/oppdater-adr-republiser")
    fun oppdaterOgRepubliserNavBrukere(
        @RequestParam(value = "batchSize", required = false) batchSize: Int = 500,
        @RequestParam(value = "modifiedBefore", required = false) modifiedBefore: LocalDateTime = LocalDateTime.now(),
        @RequestParam(value = "lastId", required = false) lastId: UUID? = null,
    ) {
        JobRunner.runAsync("oppdater-adr-republiser-nav-brukere") {
            log.info("Oppdaterer adresse for alle Nav-brukere som mangler adresse")
            oppdaterAdresseHvisManglerOgRepubliser(modifiedBefore, batchSize, lastId)
        }
    }

    @GetMapping("/nav-bruker/oppdater-adr-republiser/{id}")
    fun oppdaterAdresseOgRepubliserNavBruker(
        @PathVariable("id") id: UUID,
    ) {
        log.info("Oppdaterer adresse for Nav-bruker $id")
        val navBruker = navBrukerRepository.get(id)
        navBrukerService.oppdaterAdresse(setOf(navBruker.person.personident))
        log.info("Oppdaterte adresse for Nav-bruker $id")
    }

    @GetMapping("/nav-brukere/oppdater-innsats-republiser")
    fun oppdaterOppfolgingInnsatsOgRepubliserNavBrukere(
        @RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
        @RequestParam(value = "batchSize", required = false) batchSize: Int = 500,
        @RequestParam(value = "modifiedBefore", required = false) modifiedBefore: LocalDate? = null,
        @RequestParam(value = "lastId", required = false) lastId: UUID? = null,
    ) {
        JobRunner.runAsync("oppdater-innsats-republiser-nav-brukere") {
            if (modifiedBefore != null) {
                batchHandterNavBrukereByModifiedBefore(modifiedBefore, batchSize, lastId) {
                    navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
                }
            } else {
                batchHandterNavBrukere(startFromOffset ?: 0, batchSize) {
                    navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
                }
            }
        }
    }

    @GetMapping("/nav-bruker/oppdater-innsats-republiser/{id}")
    fun oppdaterOppfolgingInnsatsOgRepubliserNavBruker(
        @PathVariable("id") id: UUID,
    ) {
        log.info("Oppdaterer bruker $id")
        val navBruker = navBrukerRepository.get(id)
        navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(navBruker)
        log.info("Oppdaterte bruker $id")
    }

    @GetMapping("/nav-brukere/republiser/{navBrukerId}")
    fun republiserNavBruker(
        @PathVariable("navBrukerId") navBrukerId: UUID,
    ) {
        publiserNavBruker(navBrukerId)
    }

    @GetMapping("/arrangor-ansatte/republiser")
    fun republiserArrangorAnsatte(
        @RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
        @RequestParam(value = "batchSize", required = false) batchSize: Int?,
    ) {
        JobRunner.runAsync("republiser-arrangor-ansatte") {
            republiserAlleArrangorAnsatte(startFromOffset ?: 0, batchSize ?: 500)
        }
    }

    @GetMapping("/nav-ansatte/republiser")
    fun republiserNavAnsatte() {
        JobRunner.runAsync("republiser-nav-ansatte") {
            republiserAlleNavAnsatte()
        }
    }

    @GetMapping("/nav-ansatte/oppdater")
    fun oppdaterNavAnsatte() {
        JobRunner.runAsync("oppdater-nav-ansatte") {
            navAnsattUpdater.oppdaterAlle()
        }
    }

    @GetMapping("/nav-enhet/oppdater")
    fun oppdaterNavEnheter() {
        navEnhetUpdateJob.update()
    }

    @GetMapping("/nav-brukere/synkroniser-krr")
    fun synkroniserKrr(
        @RequestParam(value = "startFromOffset", required = false) startFromOffset: Int?,
        @RequestParam(value = "batchSize", required = false) batchSize: Int?,
    ) {
        JobRunner.runAsync("synkroniser-krr-nav-brukere") {
            val offset = startFromOffset ?: 0
            val limit = batchSize ?: 5000
            val personidenter =
                navBrukerRepository
                    .getPersonidenter(
                        offset = offset,
                        limit = limit,
                        notSyncedSince = LocalDateTime.now().minusDays(3),
                    ).toSet()

            navBrukerService.syncKontaktinfoBulk(personidenter)
        }
    }

    @GetMapping("/nav-brukere/oppdater-manglende-kontaktinfo")
    fun oppdaterManglendeKontakinfo(
        @RequestParam(value = "batchSize", required = false) batchSize: Int?,
    ) {
        val jobName = "oppdater-manglende-kontaktinfo"
        JobRunner.runAsync(jobName) {
            val limit = batchSize ?: 200
            var batchNumber = 1
            var personidenter: Set<String>
            var sistePersonident: String? = null

            do {
                personidenter =
                    navBrukerRepository.getPersonidenterMedManglendeKontaktinfo(sistePersonident, limit).toSet()

                if (personidenter.isNotEmpty()) {
                    log.info("Processing $jobName batch #$batchNumber count=${personidenter.size}")
                    navBrukerService.syncKontaktinfoBulk(personidenter)
                }
                batchNumber++
                sistePersonident = personidenter.lastOrNull()
            } while (personidenter.isNotEmpty())
            log.info("No more data after batch $batchNumber. Done.")
        }
    }

    @PostMapping("/nav-brukere/synkroniser-krr")
    fun synkroniserKrrForPerson(
        @RequestBody request: NavBrukerRequest,
    ) {
        val navBruker =
            navBrukerRepository.get(request.personident)
                ?: throw IllegalArgumentException("Fant ikke Nav-bruker")

        navBrukerService.oppdaterKontaktinformasjon(navBruker)
    }

    @GetMapping("/nav-brukere/republiser-ny-ident")
    fun republiserNavBrukereMedNyIdent() {
        JobRunner.runAsync("republiser-nav-brukere-med-ny-ident") {
            val personidenter = personidentRepository.getPersonIderMedFlerePersonidenter()

            log.info("Starter republisering av Nav-brukere med ny ident. Antall: ${personidenter.size}")

            personidenter.forEach {
                navBrukerRepository.getByPersonId(it)?.let { navBrukerDbo ->
                    kafkaProducerService.publiserNavBruker(navBrukerDbo)
                }
            }
            log.info("Ferdig med republisering av Nav-brukere med ny ident")
        }
    }

    private fun republiserAlleNavAnsatte() {
        val ansatte = navAnsattRepository.getAll()
        ansatte.forEach { kafkaProducerService.publiserNavAnsatt(it) }
        log.info("Publiserte ${ansatte.size} navansatte")
    }

    private fun republiserAlleArrangorAnsatte(
        startFromOffset: Int,
        batchSize: Int,
    ) {
        var offset = startFromOffset
        var ansatte: List<PersonDbo>

        do {
            ansatte = personRepository.getAllWithRolle(offset, batchSize, Rolle.ARRANGOR_ANSATT)

            ansatte.forEach { kafkaProducerService.publiserArrangorAnsatt(it) }
            log.info("Publiserte arrangøransatte fra offset $offset til ${offset + ansatte.size}")
            offset += batchSize
        } while (ansatte.isNotEmpty())
    }

    private fun batchHandterNavBrukere(
        startFromOffset: Int,
        batchSize: Int,
        action: (navBruker: NavBrukerDbo) -> Unit,
    ) {
        var currentOffset = startFromOffset
        var navBrukerDbos: List<NavBrukerDbo>

        val start = Instant.now()
        var totalHandled = 0

        do {
            navBrukerDbos = navBrukerRepository.getAllNavBrukere(currentOffset, batchSize)
            navBrukerDbos.forEach { action(it) }
            totalHandled += navBrukerDbos.size
            currentOffset += batchSize
            log.info("Republisering av nav-brukere - offset: $currentOffset, total handled: $totalHandled")
        } while (navBrukerDbos.isNotEmpty())

        val duration = Duration.between(start, Instant.now())

        if (totalHandled > 0) {
            log.info(
                "batchHandterNavBrukere handled $totalHandled Nav-bruker records in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.",
            )
        }
    }

    private fun batchHandterNavBrukereByModifiedBefore(
        modifiedBefore: LocalDate,
        batchSize: Int,
        startAfterId: UUID?,
        action: (navBruker: NavBrukerDbo) -> Unit,
    ) {
        var lastId: UUID? = startAfterId
        var navBrukerDbos: List<NavBrukerDbo>

        val start = Instant.now()
        var totalHandled = 0

        do {
            navBrukerDbos = navBrukerRepository.getAllNavBrukere(batchSize, modifiedBefore, lastId)
            navBrukerDbos.forEach { action(it) }
            totalHandled += navBrukerDbos.size
            lastId = navBrukerDbos.lastOrNull()?.id
            log.info("Handled nav-bruker batch $totalHandled records. lastId $lastId")
        } while (navBrukerDbos.isNotEmpty())

        val duration = Duration.between(start, Instant.now())

        if (totalHandled > 0) {
            log.info(
                "batchHandterNavBrukereByModifiedBefore handled $totalHandled Nav-bruker records in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.",
            )
        }
    }

    private fun oppdaterAdresseHvisManglerOgRepubliser(
        modifiedBefore: LocalDateTime,
        batchSize: Int,
        startAfterId: UUID?,
    ) {
        var lastId: UUID? = startAfterId
        var navbrukere: List<NavBrukerDbo>

        do {
            navbrukere = navBrukerRepository.getAllUtenAdresse(batchSize, modifiedBefore, lastId)
            val personidenter = navbrukere.map { it.person.personident }.toSet()
            navBrukerService.oppdaterAdresse(personidenter)

            lastId = navbrukere.lastOrNull()?.id
            log.info("Oppdaterte adresse for ${navbrukere.size} personer. Siste Nav-bruker-id: $lastId")
        } while (navbrukere.isNotEmpty())
    }

    private fun publiserNavBruker(navBrukerId: UUID) {
        val bruker = navBrukerRepository.get(navBrukerId)
        kafkaProducerService.publiserNavBruker(bruker)
    }
}
