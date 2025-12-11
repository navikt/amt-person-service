package no.nav.amt.person.service.config

import no.nav.common.rest.filter.LogRequestFilter
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.resilience.annotation.EnableResilientMethods

@EnableResilientMethods
@EnableJwtTokenValidation
@Configuration(proxyBeanMethods = false)
class ApplicationConfig {
	@Bean
	fun logFilterRegistrationBean(): FilterRegistrationBean<LogRequestFilter> =
		FilterRegistrationBean<LogRequestFilter>().apply {
			setFilter(LogRequestFilter("amt-person-service", false))
			order = 1
			addUrlPatterns("/*")
		}

	@Bean
	fun machineToMachineTokenClient(
		@Value($$"${nais.env.azureAppClientId}") azureAdClientId: String,
		@Value($$"${nais.env.azureOpenIdConfigTokenEndpoint}") azureTokenEndpoint: String,
		@Value($$"${nais.env.azureAppJWK}") azureAdJWK: String,
	): MachineToMachineTokenClient =
		AzureAdTokenClientBuilder
			.builder()
			.withClientId(azureAdClientId)
			.withTokenEndpointUrl(azureTokenEndpoint)
			.withPrivateJwk(azureAdJWK)
			.buildMachineToMachineTokenClient()

	@Bean
	fun poaoTilgangClient(
		@Value($$"${poao-tilgang.url}") poaoTilgangUrl: String,
		@Value($$"${poao-tilgang.scope}") poaoTilgangScope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
	): PoaoTilgangClient =
		PoaoTilgangCachedClient(
			PoaoTilgangHttpClient(
				baseUrl = poaoTilgangUrl,
				tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(poaoTilgangScope) },
			),
		)
}
