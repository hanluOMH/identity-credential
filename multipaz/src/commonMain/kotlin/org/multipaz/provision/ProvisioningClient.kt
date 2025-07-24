package org.multipaz.provision

import kotlinx.io.bytestring.ByteString

interface ProvisioningClient {
    suspend fun getMetadata(): ProvisioningMetadata
    suspend fun getAuthorizationChallenges(): List<AuthorizationChallenge>
    suspend fun authorize(response: AuthorizationResponse)
    suspend fun getKeyBindingChallenge(): String
    suspend fun obtainCredentials(keyInfo: BindingKeyInfo): List<ByteString>
}