package org.multipaz.provision

sealed class AuthorizationChallenge {
    abstract val id: String

    data class OAuth(override val id: String, val url: String): AuthorizationChallenge()
}