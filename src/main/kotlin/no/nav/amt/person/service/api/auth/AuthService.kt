package no.nav.amt.person.service.api.auth

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService {
    fun verifyRequestIsMachineToMachine() {
        if (!isRequestFromMachine()) {
            throw AccessDeniedException("Request is not machine-to-machine")
        }
    }

    private fun isRequestFromMachine(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("No authentication found")

        val jwt = (authentication as? JwtAuthenticationToken)?.token
            ?: throw AccessDeniedException("Not a JWT authentication")

        val sub = jwt.getClaimAsString("sub")?.let { UUID.fromString(it) }
            ?: throw AccessDeniedException("Sub is missing")

        val oid = jwt.getClaimAsString("oid")?.let { UUID.fromString(it) }
            ?: throw AccessDeniedException("Oid is missing")

        return sub == oid
    }
}
