package org.multipaz.provision.openid4vci

import org.multipaz.securearea.KeyAttestation

interface Backend {
    /**
     * Creates fresh OAuth JWT client assertion based on the server-side key.
     */
    suspend fun createJwtClientAssertion(tokenUrl: String): String

    /**
     * Creates OAuth JWT wallet attestation based on the mobile-platform-specific [KeyAttestation].
     */
    suspend fun createJwtWalletAttestation(keyAttestation: KeyAttestation): String

    /**
     * Creates OAuth JWT key attestation based on the given list of mobile-platform-specific
     * [KeyAttestation]s.
     */
    suspend fun createJwtKeyAttestation(
        keyAttestations: List<KeyAttestation>,
        challenge: String
    ): String
}