package org.multipaz.provision

sealed class CredentialFormat {
    data class Mdoc(val docType: String) : CredentialFormat()
    data class SdJwt(val vct: String) : CredentialFormat()
}