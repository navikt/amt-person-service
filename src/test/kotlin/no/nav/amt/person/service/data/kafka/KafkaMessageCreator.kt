package no.nav.amt.person.service.data.kafka

import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.message.EndringPaaBrukerMsg
import no.nav.amt.person.service.data.kafka.message.TildeltVeilederMsg
import no.nav.amt.person.service.kafka.ingestor.OpplysningsType
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.navn.Navn
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

object KafkaMessageCreator {
	fun lagEndringPaaBrukerMsg(
		fodselsnummer: String = TestData.randomIdent(),
		oppfolgingsenhet: String? = TestData.randomEnhetId(),
	) = EndringPaaBrukerMsg(
		fodlsesnummer = fodselsnummer,
		oppfolgingsenhet = oppfolgingsenhet,
	)

	fun lagTildeltVeilederMsg(
		aktorId: String = TestData.randomIdent(),
		veilederId: String = TestData.randomNavIdent(),
		tilordnet: ZonedDateTime = ZonedDateTime.now(),
	) = TildeltVeilederMsg(
		aktorId = aktorId,
		veilederId = veilederId,
		tilordnet = tilordnet
	)

	fun lagPersonhendelseAdressebeskyttelse(
		personIdenter: List<String>,
		gradering: Gradering,
	) = lagPersonhendelse(
		personIdenter = personIdenter,
		navn = null,
		adressebeskyttelse = Adressebeskyttelse(gradering),
		opplysningsType = OpplysningsType.ADRESSEBESKYTTELSE_V1,
	)

	fun lagPersonhendelseNavn(
		personIdenter: List<String>,
		fornavn: String,
		mellomnavn: String?,
		etternavn: String,
	) = lagPersonhendelse(
		personIdenter = personIdenter,
		navn = Navn(
			/* fornavn = */ fornavn,
			/* mellomnavn = */ mellomnavn,
			/* etternavn = */ etternavn,
			/* forkortetNavn = */ "forkortetNavn",
			/* originaltNavn = */ null,
			/* gyldigFraOgMed = */ LocalDate.now()
		),
		adressebeskyttelse = null,
		opplysningsType = OpplysningsType.NAVN_V1,
	)

	private fun lagPersonhendelse(
		personIdenter: List<String>,
		navn: Navn?,
		adressebeskyttelse: Adressebeskyttelse?,
		opplysningsType: OpplysningsType
	) = Personhendelse(
			/* hendelseId = */ UUID.randomUUID().toString(),
			/* personidenter = */ personIdenter,
			/* master = */ "FREG",
			/* opprettet = */ ZonedDateTime.now().toInstant(),
			/* opplysningstype = */ opplysningsType.toString(),
			/* endringstype = */ Endringstype.OPPRETTET,
			/* tidligereHendelseId = */ null,
			/* adressebeskyttelse = */ adressebeskyttelse,
			/* navn = */ navn,
	)


}
