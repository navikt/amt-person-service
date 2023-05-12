package no.nav.amt.person.service.nav_bruker.dbo

import java.util.*

data class NavBrukerKontaktinfo(
    val navBrukerId: UUID,
    val telefon: String?,
    val epost: String?,
)
