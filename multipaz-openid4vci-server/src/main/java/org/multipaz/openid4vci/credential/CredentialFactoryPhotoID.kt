package org.multipaz.openid4vci.credential

import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.EUPersonalID
import org.multipaz.rpc.backend.BackendEnvironment
import org.multipaz.rpc.backend.Resources
import org.multipaz.mdoc.mso.MobileSecurityObjectGenerator
import org.multipaz.util.toBase64Url
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearsUntil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.multipaz.cbor.RawCbor
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Uint
import org.multipaz.cbor.addCborMap
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItemFullDate
import org.multipaz.claim.JsonClaim
import org.multipaz.documenttype.DocumentAttribute
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.Icon
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.documenttype.knowntypes.PhotoID.ISO_23220_2_NAMESPACE
import org.multipaz.documenttype.knowntypes.UtopiaNaturalization
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.sdjwt.SdJwt
import org.multipaz.util.Logger
import org.multipaz.util.fromBase64Url
import kotlin.time.Duration.Companion.days

/**
 * Factory for Driver's License credentials.
 */
internal class CredentialFactoryPhotoID : CredentialFactory {
    override val offerId: String
        get() = "utopia_wholesale"

    override val scope: String
        get() = "utopia_wholesale_sd_jwt"

    override val format: Openid4VciFormat
        get() = FORMAT

    override val requireClientAttestation: Boolean get() = false

    override val requireKeyAttestation: Boolean get() = false

    override val proofSigningAlgorithms: List<String>
        get() = CredentialFactory.DEFAULT_PROOF_SIGNING_ALGORITHMS

    override val cryptographicBindingMethods: List<String>
        get() = listOf("cose_key")

    override val credentialSigningAlgorithms: List<String>
        get() = CredentialFactory.DEFAULT_CREDENTIAL_SIGNING_ALGORITHMS

    override val name: String
        get() = "Utopia Wholesale"

    override val logo: String
        get() = "card-utopia-wholesale.png"

    override suspend fun makeCredential(
        data: DataItem,
        authenticationKey: EcPublicKey?
    ): String {
        check(authenticationKey != null)
        val coreData = data["core"]

        val resources = BackendEnvironment.getInterface(Resources::class)!!
        
        // Always use john_lee.png as portrait - ensure it's base64 encoded for client display
        val portraitBytes = resources.getRawResource("john_lee.png")!!.toByteArray()
        val portrait = portraitBytes.toBase64Url()  // Convert to base64 string for client

        val identityAttributes = buildJsonObject {
            put("given_name_unicode", coreData["given_name"].asTstr)
            put("family_name_unicode", coreData["family_name"].asTstr)
            put("birth_date", coreData["birth_date"].asDateString.toString())
            put("portrait", portrait) // Portrait is already base64url encoded string
            put("issue_date", "2024-04-01")
            put("expiry_date", "2034-04-01")
            put("issuing_authority_unicode", "Utopia WholeSale Store")
            put("issuing_country", "US")
            put("document_number", "899878797979")
        }

        val now = Clock.System.now()
        val timeSigned = now
        val validFrom = Instant.parse("2024-04-01T12:00:00Z")
        val validUntil = Instant.parse("2034-04-01T12:00:00Z")

        val documentSigningKeyCert = X509Cert.fromPem(
            resources.getStringResource("ds_certificate.pem")!!
        )
        val documentSigningKey = EcPrivateKey.fromPem(
            resources.getStringResource("ds_private_key.pem")!!,
            documentSigningKeyCert.ecPublicKey
        )

        val sdJwt = SdJwt.create(
            issuerKey = documentSigningKey,
            issuerAlgorithm = documentSigningKey.curve.defaultSigningAlgorithmFullySpecified,
            issuerCertChain = X509CertChain(listOf(documentSigningKeyCert)),
            kbKey = authenticationKey,
            claims = identityAttributes,
            nonSdClaims = buildJsonObject {
                put("iss", "https://utopia.example.com")
                put("vct", UtopiaNaturalization.VCT)
                put("iat", timeSigned.epochSeconds)
                put("nbf", validFrom.epochSeconds)
                put("exp", validUntil.epochSeconds)
            }
        )
        return sdJwt.compactSerialization
    }

    companion object {
        private val FORMAT = Openid4VciFormatSdJwt(PhotoID.PHOTO_ID_DOCTYPE)
    }



}