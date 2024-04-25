package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VeilarbvedtaksstotteConfig {

	@Value("\${veilarbvedtaksstotte.url}")
	lateinit var url: String

	@Value("\${veilarbvedtaksstotte.scope}")
	lateinit var veilarbvedtaksstotteScope: String

	@Bean
	fun veilarbvedtaksstotteClient(machineToMachineTokenClient: MachineToMachineTokenClient) = VeilarbvedtaksstotteClient(
			apiUrl = "$url/veilarbvedtaksstotte",
			veilarbvedtaksstotteTokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(veilarbvedtaksstotteScope) }
		)
}
