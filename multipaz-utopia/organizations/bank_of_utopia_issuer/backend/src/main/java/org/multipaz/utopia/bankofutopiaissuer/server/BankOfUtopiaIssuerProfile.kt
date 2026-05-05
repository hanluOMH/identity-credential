package org.multipaz.utopia.bankofutopiaissuer.server

import org.multipaz.documenttype.knowntypes.addKnownTypes
import org.multipaz.openid4vci.server.UtopiaIssuerProfile
import org.multipaz.utopia.knowntypes.BankAccountCredential
import org.multipaz.utopia.knowntypes.addUtopiaTypes

object BankOfUtopiaIssuerProfile {
    val profile = UtopiaIssuerProfile(
        issuerName = "Bank of Utopia",
        credentialFactories = listOf(
            CredentialFactoryBankAccountCredential(),
        ),
        configureDocumentTypes = {
            addKnownTypes()
            addUtopiaTypes()
            addDocumentType(BankAccountCredential.getDocumentType())
        },
    )
}
