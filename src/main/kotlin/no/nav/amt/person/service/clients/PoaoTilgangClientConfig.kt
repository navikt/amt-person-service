package no.nav.amt.person.service.clients

import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class PoaoTilgangClientConfig {
    @Bean
    fun poaoTilgangClient(
        @Value($$"${poao-tilgang.url}") poaoTilgangUrl: String,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): PoaoTilgangClient {
        val clientProperties = clientConfigurationProperties.registration["poao-tilgang"]
            ?: error("Fant ikke 'poao-tilgang' i OAuth2-config")

        return PoaoTilgangCachedClient(
            PoaoTilgangHttpClient(
                baseUrl = poaoTilgangUrl,
                tokenProvider = {
                    oAuth2AccessTokenService.getAccessToken(clientProperties).access_token
                        ?: throw IllegalStateException("Fikk null access_token fra OAuth2-tjenesten for poao-tilgang")
                },
            ),
        )
    }
}
