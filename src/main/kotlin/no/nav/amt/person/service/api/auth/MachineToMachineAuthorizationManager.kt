package no.nav.amt.person.service.api.auth

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.function.Supplier

@Component
class MachineToMachineAuthorizationManager : AuthorizationManager<RequestAuthorizationContext> {
    override fun authorize(
        authentication: Supplier<out Authentication>,
        context: RequestAuthorizationContext,
    ): AuthorizationDecision {
        val jwt = authentication.get().principal as? Jwt
            ?: return AuthorizationDecision(false)

        return AuthorizationDecision(isMachineToMachine(jwt))
    }

    internal fun isMachineToMachine(jwt: Jwt): Boolean {
        val sub = jwt.getClaimAsString("sub")?.let { UUID.fromString(it) }
            ?: throw AccessDeniedException("Sub is missing")

        val oid = jwt.getClaimAsString("oid")?.let { UUID.fromString(it) }
            ?: throw AccessDeniedException("Oid is missing")

        return sub == oid
    }
}
