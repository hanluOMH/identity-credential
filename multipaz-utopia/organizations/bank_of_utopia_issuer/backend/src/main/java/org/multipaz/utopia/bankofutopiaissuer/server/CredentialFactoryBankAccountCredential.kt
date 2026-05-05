package org.multipaz.utopia.bankofutopiaissuer.server

import kotlinx.datetime.LocalDate
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.RawCbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cbor.toDataItemFullDate
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcPublicKey
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObject
import org.multipaz.openid4vci.credential.CredentialDisplay
import org.multipaz.openid4vci.credential.CredentialFactory
import org.multipaz.openid4vci.credential.MintedCredential
import org.multipaz.openid4vci.util.CredentialId
import org.multipaz.provisioning.CredentialFormat
import org.multipaz.revocation.RevocationStatus
import org.multipaz.rpc.backend.BackendEnvironment
import org.multipaz.server.common.getBaseUrl
import org.multipaz.utopia.knowntypes.BankAccountCredential
import org.multipaz.util.toBase64Url
import org.multipaz.util.truncateToWholeSeconds
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class CredentialFactoryBankAccountCredential : CredentialFactory {
    override val configurationId: String
        get() = "bank_account_mdoc"

    override val scope: String
        get() = "bank_account"

    override val format: CredentialFormat
        get() = FORMAT

    override val requireKeyAttestation: Boolean
        get() = false

    override val proofSigningAlgorithms: List<String>
        get() = CredentialFactory.DEFAULT_PROOF_SIGNING_ALGORITHMS

    override val cryptographicBindingMethods: List<String>
        get() = listOf("cose_key")

    override val name: String
        get() = "Bank account"

    override val logo: String?
        get() = null

    override suspend fun mint(
        systemOfRecordData: DataItem,
        authenticationKey: EcPublicKey?,
        credentialId: CredentialId
    ): MintedCredential {
        val now = Clock.System.now()
        val validFrom = now.truncateToWholeSeconds()
        val validUntil = validFrom + 30.days
        val data = extractData(systemOfRecordData)

        val issuerNamespaces = buildIssuerNamespaces {
            addNamespace(BankAccountCredential.BANK_ACCOUNT_NAMESPACE) {
                addDataElement("issuer_name", data["issuer_name"])
                addDataElement("account_holder_name", data["account_holder_name"])
                addDataElement("account_id", data["account_id"])
                addDataElement("masked_account_reference", data["masked_account_reference"])
                addDataElement("account_type", data["account_type"])
                addDataElement("issue_date", data["issue_date"])
                addDataElement("expiry_date", data["expiry_date"])
            }
        }

        val baseUrl = BackendEnvironment.getBaseUrl()
        val revocationStatus = RevocationStatus.StatusList(
            idx = credentialId.index,
            uri = "$baseUrl/status_list/${credentialId.bucket}",
            certificate = null
        )
        val mso = MobileSecurityObject(
            version = "1.0",
            docType = BankAccountCredential.BANK_ACCOUNT_DOCTYPE,
            signedAt = validFrom,
            validFrom = validFrom,
            validUntil = validUntil,
            expectedUpdate = null,
            digestAlgorithm = Algorithm.SHA256,
            valueDigests = issuerNamespaces.getValueDigests(Algorithm.SHA256),
            deviceKey = authenticationKey!!,
            revocationStatus = revocationStatus
        )
        val taggedEncodedMso = Cbor.encode(
            Tagged(
                Tagged.ENCODED_CBOR,
                Bstr(Cbor.encode(mso.toDataItem()))
            )
        )
        val protectedHeaders = mapOf<CoseLabel, DataItem>(
            Pair(
                CoseNumberLabel(Cose.COSE_LABEL_ALG),
                Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
            )
        )
        val signingKey = getSigningKey()
        val unprotectedHeaders = mapOf<CoseLabel, DataItem>(
            Pair(
                CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN),
                signingKey.certChain.toDataItem()
            )
        )
        val encodedIssuerAuth = Cbor.encode(
            Cose.coseSign1Sign(
                signingKey,
                taggedEncodedMso,
                true,
                protectedHeaders,
                unprotectedHeaders
            ).toDataItem()
        )
        val issuerProvidedAuthenticationData = Cbor.encode(
            buildCborMap {
                put("nameSpaces", issuerNamespaces.toDataItem())
                put("issuerAuth", RawCbor(encodedIssuerAuth))
            }
        )

        return MintedCredential(
            credential = issuerProvidedAuthenticationData.toBase64Url(),
            creation = validFrom,
            expiration = validUntil
        )
    }

    override suspend fun display(systemOfRecordData: DataItem): CredentialDisplay {
        val data = extractData(systemOfRecordData)
        return CredentialDisplay(
            title = "${data["issuer_name"].asTstr} ${data["account_type"].asTstr} account",
            cardArtUrl = null
        )
    }

    private fun extractData(systemOfRecordData: DataItem): DataItem {
        val coreData = systemOfRecordData["core"]
        val records = systemOfRecordData["records"]
        val bankAccountData = if (records.hasKey("bank_account")) {
            records["bank_account"].asMap.values.firstOrNull() ?: buildCborMap {}
        } else {
            buildCborMap {}
        }
        val given = coreData["given_name"].asTstr
        val family = coreData["family_name"].asTstr
        val accountId = if (bankAccountData.hasKey("account_id")) {
            bankAccountData["account_id"].asTstr
        } else {
            "BOU-00001234"
        }
        val issuerName = if (bankAccountData.hasKey("issuer_name")) {
            bankAccountData["issuer_name"].asTstr
        } else {
            "Bank of Utopia"
        }
        val holderName = if (bankAccountData.hasKey("account_holder_name")) {
            bankAccountData["account_holder_name"].asTstr
        } else {
            "$given $family"
        }
        val accountType = if (bankAccountData.hasKey("account_type")) {
            bankAccountData["account_type"].asTstr
        } else {
            "checking"
        }
        val issueDate = if (bankAccountData.hasKey("issue_date")) {
            bankAccountData["issue_date"]
        } else {
            LocalDate.parse("2026-01-01").toDataItemFullDate()
        }
        val expiryDate = if (bankAccountData.hasKey("expiry_date")) {
            bankAccountData["expiry_date"]
        } else {
            LocalDate.parse("2031-01-01").toDataItemFullDate()
        }
        val suffixStart = max(0, accountId.length - 4)
        val maskedAccountReference = if (bankAccountData.hasKey("masked_account_reference")) {
            bankAccountData["masked_account_reference"].asTstr
        } else {
            accountId.substring(suffixStart).padStart(accountId.length, '*')
        }

        return buildCborMap {
            put("issuer_name", issuerName)
            put("account_holder_name", holderName)
            put("account_id", accountId)
            put("masked_account_reference", maskedAccountReference)
            put("account_type", accountType)
            put("issue_date", issueDate)
            put("expiry_date", expiryDate)
        }
    }

    companion object {
        private val FORMAT = CredentialFormat.Mdoc(BankAccountCredential.BANK_ACCOUNT_DOCTYPE)
    }
}
