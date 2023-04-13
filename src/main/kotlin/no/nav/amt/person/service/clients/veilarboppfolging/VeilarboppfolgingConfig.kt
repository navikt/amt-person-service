package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VeilarboppfolgingConfig {

	@Value("\${veilarboppfolging.url}")
	lateinit var url: String

	@Value("\${veilarboppfolging.scope}")
	lateinit var veilarboppfolgingScope: String

	@Bean
	fun veilarboppfolgingClient(machineToMachineTokenClient: MachineToMachineTokenClient) = VeilarboppfolgingClient(
			apiUrl = "$url/veilarboppfolging",
			veilarboppfolgingTokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(veilarboppfolgingScope) }
		)
}
