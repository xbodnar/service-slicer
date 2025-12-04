package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class SecurityProperties(
    val enabled: Boolean = false,
    val admin: AdminProperties = AdminProperties(),
    val cors: CorsProperties = CorsProperties(),
) {
    data class AdminProperties(
        val username: String = "admin",
        val password: String = "admin",
    )

    data class CorsProperties(
        val allowedOriginPatterns: List<String> = listOf("http://localhost:3000"),
    )
}
