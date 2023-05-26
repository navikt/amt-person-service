package no.nav.amt.person.service.controller.migrering

import no.nav.amt.person.service.controller.auth.Issuer
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/migrer")
class MigreringController(
	private val navEnhetService: NavEnhetService,
	private val navAnsattService: NavAnsattService,
	private val navBrukerService: NavBrukerService,
	private val migreringRepository: MigreringRepository,
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-enhet")
	fun migrerNavEnhet(
		@RequestBody request: MigreringNavEnhet,
	) {
		val enhet = navEnhetService.hentEllerOpprettNavEnhetMedId(request.enhetId, request.id)

		if (enhet == null) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-enhet",
					requestBody = JsonUtils.toJsonString(request),
					diff = null,
					error = "Fant ikke NavEnhet"
				)
			)
			return
		}

		val diffMap = request.diff(enhet)

		if (diffMap.isNotEmpty()) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-enhet",
					requestBody = JsonUtils.toJsonString(request),
					diff = JsonUtils.toJsonString(diffMap),
					error = null,
				)
			)
		}
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-ansatt")
	fun migrerNavAnsatt(
		@RequestBody request: MigreringNavAnsatt,
	) {
		try {
			val ansatt = navAnsattService.hentEllerOpprettAnsatt(request.navIdent, request.id)

			val diffMap = request.diff(ansatt)

			if (diffMap.isNotEmpty()) {
				migreringRepository.upsert(
					MigreringDbo(
						resursId = request.id,
						endepunkt = "nav-ansatt",
						requestBody = JsonUtils.toJsonString(request),
						diff = JsonUtils.toJsonString(diffMap),
						error = null,
					)
				)
			}
		} catch (e: Exception) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-ansatt",
					requestBody = JsonUtils.toJsonString(request),
					diff = null,
					error = e.message
				)
			)
			throw (e)
		}
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker")
	fun migrerNavBruker(
		@RequestBody request: MigreringNavBruker,
	) {
		try {
		    val bruker = navBrukerService.hentEllerOpprettNavBruker(request.personIdent, request.id)
			val diffMap = request.diff(bruker)

			if (diffMap.isNotEmpty()) {
				migreringRepository.upsert(
					MigreringDbo(
						resursId = request.id,
						endepunkt = "nav-bruker",
						requestBody = JsonUtils.toJsonString(request),
						diff = JsonUtils.toJsonString(diffMap),
						error = null,
					)
				)
			}
		} catch (e: Exception) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-bruker",
					requestBody = JsonUtils.toJsonString(request),
					diff = null,
					error = e.message
				)
			)
			throw (e)
		}

	}
}

data class MigreringNavEnhet(
	val id: UUID,
	val enhetId: String,
	val navn: String,
) {
	fun diff(navEnhet: NavEnhet): Map<String, DiffProperty> {
		val diffMap = mutableMapOf<String, DiffProperty>()
		if (this.id != navEnhet.id) diffMap["id"] = DiffProperty(this.id.toString(), navEnhet.id.toString())
		if (this.enhetId != navEnhet.enhetId) diffMap["enhetId"] = DiffProperty(this.enhetId, navEnhet.enhetId)
		if (this.navn != navEnhet.navn) diffMap["navn"] = DiffProperty(this.navn, navEnhet.navn)

		return diffMap
	}
}

data class MigreringNavAnsatt(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
) {
	fun diff(navAnsatt: NavAnsatt): Map<String, DiffProperty> {
		val diffMap = mutableMapOf<String, DiffProperty>()
		if (this.id != navAnsatt.id) diffMap["id"] = DiffProperty(this.id.toString(), navAnsatt.id.toString())
		if (this.navIdent != navAnsatt.navIdent) diffMap["navIdent"] = DiffProperty(this.navIdent, navAnsatt.navIdent)
		if (this.navn != navAnsatt.navn) diffMap["navn"] = DiffProperty(this.navn, navAnsatt.navn)
		if (this.epost != navAnsatt.epost) diffMap["epost"] = DiffProperty(this.epost, navAnsatt.epost)
		if (this.telefon != navAnsatt.telefon) diffMap["telefon"] = DiffProperty(this.telefon, navAnsatt.telefon)

		return diffMap
	}
}


data class MigreringNavBruker(
	val id: UUID,
	val personIdent: String,
	val personIdentType: String?,
	val historiskeIdenter: List<String>,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhetId: UUID?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
) {
	fun diff(navBruker: NavBruker): Map<String, DiffProperty> {

		val diffMap = mutableMapOf<String, DiffProperty>()

		if (this.personIdent != navBruker.person.personIdent)
			diffMap["personIdent"] = DiffProperty(this.personIdent, navBruker.person.personIdent)
		if (navBruker.person.personIdentType != null && this.personIdentType.toString() != navBruker.person.personIdentType.toString())
			diffMap["personIdentType"] = DiffProperty(this.personIdentType.toString(), navBruker.person.personIdentType.toString())
		if (this.fornavn != navBruker.person.fornavn)
			diffMap["fornavn"] = DiffProperty(this.fornavn, navBruker.person.fornavn)
		if (this.mellomnavn != navBruker.person.mellomnavn)
			diffMap["mellomnavn"] = DiffProperty(this.mellomnavn, navBruker.person.mellomnavn)
		if (this.etternavn != navBruker.person.etternavn)
			diffMap["etternavn"] = DiffProperty(this.etternavn, navBruker.person.etternavn)
		if (this.telefon != navBruker.telefon)
			diffMap["telefon"] = DiffProperty(this.telefon, navBruker.telefon)
		if (this.epost != navBruker.epost)
			diffMap["epost"] = DiffProperty(this.epost, navBruker.epost)
		if (this.erSkjermet != navBruker.erSkjermet)
			diffMap["erSkjermet"] = DiffProperty(this.erSkjermet.toString(), navBruker.erSkjermet.toString())
		if (this.navEnhetId != navBruker.navEnhet?.id)
			diffMap["navEnhetId"] = DiffProperty(this.navEnhetId.toString(), navBruker.navEnhet?.id.toString())
		if (this.navVeilederId != navBruker.navVeileder?.id)
			diffMap["navVeilederId"] = DiffProperty(this.navVeilederId.toString(), navBruker.navVeileder?.id.toString())

		return diffMap
	}
}

data class DiffProperty(
	val amtTiltak: String?,
	val amtPerson: String?,
)


