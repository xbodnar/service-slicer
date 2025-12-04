package cz.bodnor.serviceslicer.adapter.`in`.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Profile("!local")
class AuthController(
    private val authenticationManager: AuthenticationManager,
) {
    private val securityContextRepository: SecurityContextRepository = HttpSessionSecurityContextRepository()

    data class LoginRequest(
        val username: String,
        val password: String,
    )

    data class AuthResponse(
        val username: String,
        val authenticated: Boolean,
    )

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<AuthResponse> = try {
        val authToken = UsernamePasswordAuthenticationToken(request.username, request.password)
        val authentication: Authentication = authenticationManager.authenticate(authToken)

        // Create new security context and set authentication
        val context: SecurityContext = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)

        // Save the security context in the session
        securityContextRepository.saveContext(context, httpRequest, httpResponse)

        ResponseEntity.ok(
            AuthResponse(
                username = authentication.name,
                authenticated = true,
            ),
        )
    } catch (e: Exception) {
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    @PostMapping("/logout")
    fun logout(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<Void> {
        SecurityContextHolder.clearContext()
        httpRequest.session?.invalidate()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<AuthResponse> {
        val authentication = SecurityContextHolder.getContext().authentication

        return if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            ResponseEntity.ok(
                AuthResponse(
                    username = authentication.name,
                    authenticated = true,
                ),
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
