package org.multipaz.openid4vci.customization

import org.multipaz.cbor.DataItem
import org.multipaz.openid4vci.util.CredentialId
import org.multipaz.rpc.backend.BackendEnvironment

/**
 * Optional observer for successful OpenID4VCI credential issuance.
 *
 * Issuer-specific servers can register this interface in [BackendEnvironment] to perform
 * side effects such as audit logging or notifying a System of Record. If absent, issuance
 * behavior is unchanged.
 */
interface IssuanceObserver {
    suspend fun onIssued(
        systemOfRecordData: DataItem,
        credentialId: CredentialId,
        configurationId: String,
    )

    companion object {
        suspend fun notifyIssued(
            systemOfRecordData: DataItem,
            credentialId: CredentialId,
            configurationId: String,
        ) {
            BackendEnvironment.getInterface(IssuanceObserver::class)?.onIssued(
                systemOfRecordData = systemOfRecordData,
                credentialId = credentialId,
                configurationId = configurationId,
            )
        }
    }
}
