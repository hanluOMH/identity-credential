package org.multipaz.openid4vci.server

import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.addKnownTypes
import org.multipaz.openid4vci.credential.CredentialFactory
import org.multipaz.openid4vci.credential.CredentialFactoryRegistry
import org.multipaz.openid4vci.customization.IssuanceObserver
import org.multipaz.openid4vci.customization.NonceManager
import org.multipaz.server.common.ServerEnvironmentInitializer
import org.multipaz.utopia.knowntypes.addUtopiaTypes

/**
 * Configuration data for a Utopia OpenID4VCI issuer.
 *
 * This is intentionally not a server superclass. It only captures the small set of
 * issuer-selected interfaces that are registered into the server environment.
 */
data class UtopiaIssuerProfile(
    val issuerName: String,
    val issuerLocale: String = "en-US",
    val credentialFactories: List<CredentialFactory>,
    val configureDocumentTypes: DocumentTypeRepository.() -> Unit = {
        addKnownTypes()
        addUtopiaTypes()
    },
    val nonceManager: NonceManager? = null,
    val issuanceObserver: IssuanceObserver? = null,
)

/**
 * Registers the issuer-selected OpenID4VCI and Utopia interfaces.
 */
suspend fun ServerEnvironmentInitializer.addUtopiaIssuer(profile: UtopiaIssuerProfile) {
    val credentialFactoryRegistry = CredentialFactoryRegistry(profile.credentialFactories)
    credentialFactoryRegistry.initialize()
    add(CredentialFactoryRegistry::class, credentialFactoryRegistry)

    val documentTypeRepository = DocumentTypeRepository().apply(profile.configureDocumentTypes)
    add(DocumentTypeRepository::class, documentTypeRepository)

    profile.nonceManager?.let { add(NonceManager::class, it) }
    profile.issuanceObserver?.let { add(IssuanceObserver::class, it) }
}
