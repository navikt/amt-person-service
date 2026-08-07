package no.nav.amt.person.service.config

import no.nav.amt.person.service.api.auth.InternalAuthorizationManager
import no.nav.amt.person.service.api.auth.MachineToMachineAuthorizationManager
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.disable
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusScrapeEndpoint
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration(proxyBeanMethods = false)
class SecurityConfig {
    @Bean
    fun oauth2AuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrationRepository,
        authorizedClientService,
    ).apply {
        setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build(),
        )
    }

    @Bean
    fun oauth2Configurer(manager: OAuth2AuthorizedClientManager) = OAuth2RestClientHttpServiceGroupConfigurer.from(manager)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        machineToMachineAuthorizationManager: MachineToMachineAuthorizationManager,
        internalAuthorizationManager: InternalAuthorizationManager,
    ): SecurityFilterChain {
        http {
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            csrf { disable() }
            logout { disable() }
            requestCache { disable() }
            oauth2ResourceServer { jwt { } }
            authorizeHttpRequests {
                authorize(
                    OrRequestMatcher(
                        EndpointRequest.to(HealthEndpoint::class.java),
                        EndpointRequest.to(PrometheusScrapeEndpoint::class.java),
                    ),
                    permitAll,
                )
                authorize("/api/**", machineToMachineAuthorizationManager)
                authorize("/internal/**", internalAuthorizationManager)
                authorize(anyRequest, authenticated)
            }
        }

        return http.build()
    }
}
