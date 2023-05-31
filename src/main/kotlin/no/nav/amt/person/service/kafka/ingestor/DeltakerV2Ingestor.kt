package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.clients.amt_tiltak.AmtTiltakClient
import no.nav.amt.person.service.migrering.MigreringNavBruker
import no.nav.amt.person.service.migrering.MigreringService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class DeltakerV2Ingestor(
	private val migreringService: MigreringService,
	private val amtTiltakClient: AmtTiltakClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	fun ingest(key: String, value: String?) {
		if (value == null) {
			log.info("Fikk tombstone på key: $key hopper over deltaker")
			return
		}

		val deltakerDto = fromJsonString<DeltakerDto>(value)
		val brukerInfo = amtTiltakClient.hentBrukerInfo(deltakerDto.id)

		migreringService.migrerNavBruker(MigreringNavBruker(
			id = brukerInfo.brukerId,
			personIdent = deltakerDto.personalia.personident,
			personIdentType = brukerInfo.personIdentType.name,
			historiskeIdenter = brukerInfo.historiskeIdenter,
			fornavn = deltakerDto.personalia.navn.fornavn,
			mellomnavn = deltakerDto.personalia.navn.mellomnavn,
			etternavn = deltakerDto.personalia.navn.etternavn,
			navVeilederId = deltakerDto.navVeileder?.id,
			navEnhetId = brukerInfo.navEnhetId,
			telefon = deltakerDto.personalia.kontaktinformasjon.epost,
			epost = deltakerDto.personalia.kontaktinformasjon.epost,
			erSkjermet = deltakerDto.personalia.skjermet,
		))
	}
}

data class DeltakerDto(
	val id: UUID,
	val personalia: DeltakerPersonaliaDto,
	val navVeileder: DeltakerNavVeilederDto?,
) {
	data class DeltakerPersonaliaDto(
		val personident: String,
		val navn: NavnDto,
		val kontaktinformasjon: DeltakerKontaktinformasjonDto,
		val skjermet: Boolean
	) {
		data class NavnDto(
			val fornavn: String,
			val mellomnavn: String?,
			val etternavn: String
		)

		data class DeltakerKontaktinformasjonDto(
			val telefonnummer: String?,
			val epost: String?
		)
	}

	data class DeltakerNavVeilederDto(
		val id: UUID,
		val navn: String,
		val epost: String?
	)
}
