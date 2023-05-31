package no.nav.amt.person.service.clients.amt_tiltak

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AmtTiltakClientConfig {

	@Value("\${amt-tiltak.url}")
	lateinit var url: String

	@Value("\${amt-tiltak.scope}")
	lateinit var scope: String

	@Bean
	fun amtTiltakClient(machineToMachineTokenClient: MachineToMachineTokenClient) = AmtTiltakClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
	)
}
