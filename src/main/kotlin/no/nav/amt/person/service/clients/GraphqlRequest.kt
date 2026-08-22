package no.nav.amt.person.service.clients

data class GraphqlRequest(
    val query: String,
    val variables: Any,
)
