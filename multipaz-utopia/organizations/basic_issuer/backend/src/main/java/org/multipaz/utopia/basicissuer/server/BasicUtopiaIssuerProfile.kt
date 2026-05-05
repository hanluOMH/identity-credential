package org.multipaz.utopia.basicissuer.server

import org.multipaz.openid4vci.credential.CredentialFactoryDigitalPaymentCredential
import org.multipaz.openid4vci.credential.CredentialFactoryUtopiaLoyalty
import org.multipaz.openid4vci.server.UtopiaIssuerProfile

object BasicUtopiaIssuerProfile {
    val profile = UtopiaIssuerProfile(
        issuerName = "Basic Utopia Issuer",
        credentialFactories = listOf(
            CredentialFactoryDigitalPaymentCredential(),
            CredentialFactoryUtopiaLoyalty(),
        ),
    )
}
