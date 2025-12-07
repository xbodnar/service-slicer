package cz.bodnor.serviceslicer.adapter.`in`.web.auth

data class AuthResponse(
    val username: String,
    val authenticated: Boolean,
)
