package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.util.fromBase64Url

/**
 * An abstract object that describes transaction data item.
 *
 * @param hash hash of encoded transaction data item, calculated using [hashAlgorithm] or SHA256
 *  if not explicitly specified
 * @param hashAlgorithm algorithm, only if it was explicitly specified in transaction data
 * @param type type of the transaction data item
 */
sealed class TransactionData(
    val hash: ByteString,
    val hashAlgorithm: Algorithm?,
    val type: String
)