package org.multipaz.transactiontype.knowntypes

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.toDataItem
import org.multipaz.credential.Credential
import org.multipaz.documenttype.DocumentAttribute
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.MdocDataElement
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential
import org.multipaz.util.fromBase64Url

/**
 * Transaction type that round-trips some data through the presentment process for testing.
 */
object PingTransaction: TransactionType(
    displayName = "Ping",
    identifier = "org.multipaz.transaction.ping",
    attributes = listOf(
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "string",
                type = DocumentAttributeType.String,
                displayName = "String data",
                description = "String data to round-trip"
            ),
            mandatory = false
        ),
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "blob",
                type = DocumentAttributeType.Blob,
                displayName = "Binary data",
                description = "Binary data to round-trip"
            ),
            mandatory = false
        )
    )
) {
    /** Same as [org.multipaz.documenttype.knowntypes.UtopiaNaturalization.VCT] (defined in multipaz-utopia). */
    private const val UTOPIA_NATURALIZATION_VCT = "http://utopia.example.com/vct/naturalization"

    override suspend fun isApplicable(
        transactionData: TransactionData,
        credential: Credential
    ): Boolean {
        // For the sake of testing, refuse Utopia naturalization credentials
        return !(credential is KeyBoundSdJwtVcCredential
                && credential.vct == UTOPIA_NATURALIZATION_VCT)
    }

    override suspend fun applyCbor(
        transactionData: TransactionData,
        credential: Credential
    ): Map<String, DataItem> {
        return buildMap {
            val stringValue = transactionData.jsonData?.get("string")?.jsonPrimitive?.contentOrNull
                ?: transactionData.cborData?.let { cbor ->
                    if (cbor.hasKey("string")) cbor["string"].asTstr else null
                }
            stringValue?.let { put("string", it.toDataItem()) }

            val blobBytes = transactionData.jsonData?.get("blob")?.jsonPrimitive?.contentOrNull
                ?.fromBase64Url()
                ?: transactionData.cborData?.let { cbor ->
                    if (cbor.hasKey("blob")) cbor["blob"].asBstr else null
                }
            blobBytes?.let { put("blob", it.toDataItem()) }
        }
    }
}