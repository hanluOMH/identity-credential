package org.multipaz.provision

import org.multipaz.securearea.KeyAttestation

sealed class BindingKeyInfo {
    data object Keyless: BindingKeyInfo()

    data class OpenidProofOfPossession(
        val jwtList: List<String>
    )

    data class Attestation(
        val attestations: List<KeyAttestation>
    ): BindingKeyInfo()
}