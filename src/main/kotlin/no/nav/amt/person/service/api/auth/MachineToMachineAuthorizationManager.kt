package no.nav.amt.person.service.api.auth

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

    companion object {
        private const val SUB_CLAIM = "sub"
        private const val OID_CLAIM = "oid"

        internal fun isMachineToMachine(jwt: Jwt): Boolean {
            val sub = runCatching {
                jwt.getClaimAsString(SUB_CLAIM)?.let(UUID::fromString)
            }.getOrNull() ?: return false

            val oid = runCatching {
                jwt.getClaimAsString(OID_CLAIM)?.let(UUID::fromString)
            }.getOrNull() ?: return false

            return sub == oid
        }
    }
}
