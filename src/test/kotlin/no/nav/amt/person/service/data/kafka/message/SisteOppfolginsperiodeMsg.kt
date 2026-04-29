package no.nav.amt.person.service.data.kafka.message

data class SisteOppfolginsperiodeMsg(
    val ident: String,
    val kontor: KontorMsg,
    val sisteEndringsType: String,
    val sluttTidspunkt: String? = null,
)

data class KontorMsg(
    val kontorId: String,
    val kontorNavn: String,
)
