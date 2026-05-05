package org.multipaz.utopia.bankofutopiaissuer.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.putCborMap
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.openid4vci.server.addUtopiaIssuer
import org.multipaz.openid4vci.server.configureRouting
import org.multipaz.openid4vci.util.CredentialId
import org.multipaz.rpc.backend.Configuration
import org.multipaz.server.common.ServerConfiguration
import org.multipaz.server.common.ServerEnvironment
import org.multipaz.server.common.installServerEnvironment
import org.multipaz.utopia.knowntypes.BankAccountCredential
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential

class BankOfUtopiaIssuerTest {
    @Test
    fun issuerMetadataIncludesOnlyBankAccountCredential() = testApplication {
        val serverEnvironment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BankOfUtopiaIssuerProfile.profile)
        }
        application {
            installServerEnvironment(serverEnvironment)
            configureRouting(serverEnvironment)
        }

        val response = client.get("/.well-known/openid-credential-issuer")

        assertEquals(HttpStatusCode.OK, response.status)
        val metadata = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val supported = metadata["credential_configurations_supported"]!!.jsonObject
        assertEquals(setOf("bank_account_mdoc"), supported.keys)
    }

    @Test
    fun registersGenericUtopiaAndBankDocumentTypes() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BankOfUtopiaIssuerProfile.profile)
        }.await()

        val repository = environment.getInterface(DocumentTypeRepository::class)!!

        assertNotNull(repository.getDocumentTypeForMdoc(DrivingLicense.MDL_DOCTYPE))
        assertNotNull(repository.getDocumentTypeForMdoc(DigitalPaymentCredential.CARD_DOCTYPE))
        assertNotNull(repository.getDocumentTypeForMdoc(BankAccountCredential.BANK_ACCOUNT_DOCTYPE))
    }

    @Test
    fun canMintDemoBankAccountCredential() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BankOfUtopiaIssuerProfile.profile)
        }.await()
        val factory = CredentialFactoryBankAccountCredential()
        val authenticationKey = Crypto.createEcPrivateKey(EcCurve.P256).publicKey
        val data = buildCborMap {
            putCborMap("core") {
                put("given_name", "Ada")
                put("family_name", "Lovelace")
            }
            putCborMap("records") {}
        }

        val minted = withContext(environment) {
            factory.mint(data, authenticationKey, CredentialId("bank-test", 1))
        }
        val display = withContext(environment) {
            factory.display(data)
        }

        assertTrue(minted.credential.isNotBlank())
        assertEquals("Bank of Utopia checking account", display.title)
    }

    @Test
    fun preservesBankIssuerConfigurationValues() = runTest {
        val environment = ServerEnvironment.create(serverConfiguration()) {
            addUtopiaIssuer(BankOfUtopiaIssuerProfile.profile)
        }.await()

        val configuration = environment.getInterface(Configuration::class)!!

        assertEquals("Bank of Utopia", configuration.getValue("issuer_name"))
        assertEquals("en-US", configuration.getValue("issuer_locale"))
    }

    private fun serverConfiguration() = ServerConfiguration(
        arrayOf(
            "-param", "base_url=http://localhost",
            "-param", "database_engine=ephemeral",
            "-param", "issuer_name=${BankOfUtopiaIssuerProfile.profile.issuerName}",
            "-param", "issuer_locale=${BankOfUtopiaIssuerProfile.profile.issuerLocale}",
        )
    )
}
