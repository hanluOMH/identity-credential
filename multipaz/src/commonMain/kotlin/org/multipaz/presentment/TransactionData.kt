package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.CborMap
import org.multipaz.crypto.Algorithm

/**
 * Model for a transaction data item.
 *
 * @param hash hash of encoded transaction data item, calculated using [hashAlgorithm] or SHA256
 *  if not explicitly specified
 * @param hashAlgorithm algorithm, only if it was explicitly specified in transaction data
 * @param type type of the transaction data item
 * @param jsonData JSON payload when parsed from OpenID4VP `transaction_data`
 * @param cborData CBOR payload when parsed from ISO mdoc `transactionData`
 */
data class TransactionData(
    val hash: ByteString,
    val hashAlgorithm: Algorithm?,
    val type: String,
    val jsonData: JsonObject? = null,
    val cborData: CborMap? = null
)
