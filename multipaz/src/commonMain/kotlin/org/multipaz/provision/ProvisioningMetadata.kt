package org.multipaz.provision

data class ProvisioningMetadata(
    val display: Display,
    val credentials: Map<String, CredentialMetadata>
)