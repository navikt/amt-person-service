package no.nav.amt.person.service.clients.oppfolgningskontor

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class OppfolgningskontorConfig {
    @Bean
    fun oppfolgningskontorClient(
        @Value($$"${ao-oppfolgingskontor.url}") url: String,
        @Value($$"${ao-oppfolgingskontor.scope}") scope: String,
        machineToMachineTokenClient: MachineToMachineTokenClient,
        objectMapper: ObjectMapper,
    ) = OppfolgningskontorClient(
        baseUrl = url,
        tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
        objectMapper = objectMapper,
    )
}
