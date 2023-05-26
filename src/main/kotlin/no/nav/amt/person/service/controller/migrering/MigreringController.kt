package no.nav.amt.person.service.controller.migrering

import no.nav.amt.person.service.controller.auth.Issuer
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
	private val migreringRepository: MigreringRepository,
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-enhet")
	fun migrerNavEnhet(
		@RequestBody request: MigreringNavEnhet,
	) {
		val enhet = navEnhetService.hentEllerOpprettNavEnhetMedId(request.enhetId, request.id)

		if (enhet == null) {
			migreringRepository.upsert(MigreringDbo(request.id, "nav-enhet", JsonUtils.toJsonString(request), null))
			return
		}

		val diffMap = request.diff(enhet)

		if (diffMap.isNotEmpty()) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-enhet",
					requestBody = JsonUtils.toJsonString(request),
					diff = JsonUtils.toJsonString(diffMap)
				)
			)
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

data class DiffProperty(
	val amtTiltak: String?,
	val amtPerson: String?,
)


