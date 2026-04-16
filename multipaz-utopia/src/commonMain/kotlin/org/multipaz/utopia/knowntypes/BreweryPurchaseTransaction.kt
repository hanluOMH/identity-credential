package org.multipaz.utopia.knowntypes

import org.multipaz.cbor.DataItem
import org.multipaz.cbor.toDataItem
import org.multipaz.credential.Credential
import org.multipaz.documenttype.DocumentAttribute
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.MdocDataElement
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData

/**
 * Transaction type for Utopia Brewery purchase authorization.
 *
 * Carries structured purchase details (merchant, product description, amount, currency)
 * through the credential presentation flow. The wallet echoes these fields back in the
 * device-signed namespace so the verifier can confirm what the holder authorized.
 */
object BreweryPurchaseTransaction : TransactionType(
    displayName = "Brewery Purchase",
    identifier = "org.multipaz.transaction.brewery.purchase",
    attributes = listOf(
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "merchant",
                type = DocumentAttributeType.String,
                displayName = "Merchant",
                description = "Name of the merchant initiating the transaction"
            ),
            mandatory = true
        ),
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "description",
                type = DocumentAttributeType.String,
                displayName = "Description",
                description = "Product or item being purchased"
            ),
            mandatory = true
        ),
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "amount",
                type = DocumentAttributeType.String,
                displayName = "Amount",
                description = "Purchase amount as a decimal string (e.g. \"72.00\")"
            ),
            mandatory = true
        ),
        MdocDataElement(
            attribute = DocumentAttribute(
                identifier = "currency",
                type = DocumentAttributeType.String,
                displayName = "Currency",
                description = "ISO 4217 currency code (e.g. \"USD\")"
            ),
            mandatory = true
        )
    )
) {
    /** Applicable to any credential — scoping to the payment credential is done via credential_ids. */
    override suspend fun isApplicable(
        transactionData: TransactionData,
        credential: Credential
    ): Boolean = true

    /** Round-trip all four fields through the device-signed namespace. */
    override suspend fun applyCbor(
        transactionData: TransactionData,
        credential: Credential
    ): Map<String, DataItem> = buildMap {
        val attrs = transactionData.attributes
        attrs.getString("merchant")?.let    { put("merchant",     it.toDataItem()) }
        attrs.getString("description")?.let { put("description",  it.toDataItem()) }
        attrs.getString("amount")?.let      { put("amount",       it.toDataItem()) }
        attrs.getString("currency")?.let    { put("currency",     it.toDataItem()) }
    }
}
