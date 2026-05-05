package org.multipaz.utopia.basicissuer.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.openid4vci.credential.CredentialFactoryRegistry
import org.multipaz.openid4vci.customization.IssuanceObserver
import org.multipaz.openid4vci.server.configureRouting
import org.multipaz.rpc.backend.Configuration
import org.multipaz.server.common.ServerConfiguration
import org.multipaz.server.common.ServerEnvironment
import org.multipaz.server.common.installServerEnvironment
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential
import org.multipaz.utopia.knowntypes.Loyalty

class BasicUtopiaIssuerTest {
    @Test
    fun issuerMetadataIncludesOnlyBasicCredentialFactories() = testApplication {
        val serverEnvironment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
        }
        application {
            installServerEnvironment(serverEnvironment)
            configureRouting(serverEnvironment)
        }

        val response = client.get("/.well-known/openid-credential-issuer")

        assertEquals(HttpStatusCode.OK, response.status)
        val metadata = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val supported = metadata["credential_configurations_supported"]!!.jsonObject
        assertEquals(
            setOf("payment_sca_mdoc", "utopia_wholesale"),
            supported.keys
        )
    }

    @Test
    fun registersGenericAndUtopiaDocumentTypes() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
        }.await()

        val repository = environment.getInterface(DocumentTypeRepository::class)!!

        assertNotNull(repository.getDocumentTypeForMdoc(DrivingLicense.MDL_DOCTYPE))
        assertNotNull(repository.getDocumentTypeForMdoc(DigitalPaymentCredential.CARD_DOCTYPE))
        assertNotNull(repository.getDocumentTypeForMdoc(Loyalty.LOYALTY_DOCTYPE))
    }

    @Test
    fun issuanceObserverIsOptional() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
        }.await()

        assertNull(environment.getInterface(IssuanceObserver::class))
    }

    @Test
    fun registersIssuanceObserverWhenProvided() = runTest {
        val observer = TestIssuanceObserver()
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(
                BasicUtopiaIssuerProfile.profile.copy(
                    issuanceObserver = observer
                )
            )
        }.await()

        assertEquals(observer, environment.getInterface(IssuanceObserver::class))
    }

    @Test
    fun registersBasicCredentialFactoryRegistry() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
        }.await()

        val registry = environment.getInterface(CredentialFactoryRegistry::class)!!

        assertEquals(
            setOf("payment_sca_mdoc", "utopia_wholesale"),
            registry.byId.keys
        )
        assertEquals(setOf("payment", "wholesale"), registry.supportedScopes)
    }

    @Test
    fun preservesIssuerConfigurationValues() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)
        }.await()

        val configuration = environment.getInterface(Configuration::class)!!

        assertEquals("Basic Utopia Issuer", configuration.getValue("issuer_name"))
        assertEquals("en-US", configuration.getValue("issuer_locale"))
    }

    private fun serverConfiguration() = ServerConfiguration(
        arrayOf(
            "-param", "base_url=http://localhost",
            "-param", "database_engine=ephemeral",
            "-param", "issuer_name=${BasicUtopiaIssuerProfile.profile.issuerName}",
            "-param", "issuer_locale=${BasicUtopiaIssuerProfile.profile.issuerLocale}",
        )
    )

    private class TestIssuanceObserver : IssuanceObserver {
        override suspend fun onIssued(
            systemOfRecordData: org.multipaz.cbor.DataItem,
            credentialId: org.multipaz.openid4vci.util.CredentialId,
            configurationId: String,
        ) {
            // Not used by these registration-focused tests.
        }
    }
}
