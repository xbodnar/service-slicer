package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class SecurityProperties(
    val enabled: Boolean = false,
    val admin: AdminProperties,
    val cors: CorsProperties,
) {
    data class AdminProperties(
        val username: String,
        val password: String,
    )

    data class CorsProperties(
        val allowedOriginPatterns: List<String>,
    )
}
