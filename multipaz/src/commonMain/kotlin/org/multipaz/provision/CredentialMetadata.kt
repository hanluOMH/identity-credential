package org.multipaz.provision

data class CredentialMetadata(
    val display: Display,
    val format: CredentialFormat,
    val keyProofType: KeyProofType,
    val maxBatchSize: Int
)
