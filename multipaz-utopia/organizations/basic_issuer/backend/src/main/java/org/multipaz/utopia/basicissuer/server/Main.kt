package org.multipaz.utopia.basicissuer.server

import org.multipaz.openid4vci.server.configureRouting
import org.multipaz.server.common.ServerConfiguration
import org.multipaz.server.common.runServer

/**
 * Main entry point for the Basic Utopia OpenID4VCI issuer.
 *
 * Build and start the server using:
 *
 * ```./gradlew multipaz-utopia:organizations:basic_issuer:backend:run```
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runServer(
                args = args,
                needAdminPassword = true,
                checkConfiguration = ::checkConfiguration,
                environmentInitializer = {
                    addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
                }
            ) { environment ->
                configureRouting(environment)
            }
        }

        private fun checkConfiguration(configuration: ServerConfiguration) {
            val supportClientAssertion = configuration.getValue("support_client_assertion") != "false"
            val supportClientAttestation = configuration.getValue("support_client_attestation") != "false"
            if (!supportClientAssertion && !supportClientAttestation) {
                throw IllegalArgumentException("No client authentication methods supported")
            }
        }
    }
}
