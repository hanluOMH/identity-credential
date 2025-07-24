package org.multipaz.provision

import org.multipaz.crypto.Algorithm

sealed class KeyProofType {
    object Keyless: KeyProofType()

    data class ProofOfPossession(
        val algorithm: Algorithm
    ): KeyProofType()

    data class Attestation(
        val algorithm: Algorithm
    ): KeyProofType()
}