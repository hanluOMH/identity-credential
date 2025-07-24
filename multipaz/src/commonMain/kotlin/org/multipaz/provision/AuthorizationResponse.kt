package org.multipaz.provision

sealed class AuthorizationResponse {
    abstract val id: String
    data class OAuth(
        override val id: String,
        val parameterizedRedirectUrl: String
    ): AuthorizationResponse()
}