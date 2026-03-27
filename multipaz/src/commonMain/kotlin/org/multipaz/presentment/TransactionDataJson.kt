package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.util.fromBase64Url

/**
 * Parser utilities for OpenID4VP JSON-encoded transaction data.
 */
object TransactionDataJson {
    /**
     * Parses OpenID4VP JSON-encoded transaction data.
     *
     * @param transactionData encoded transaction data (array of base64url-encoded JSON items)
     * @return map of credential id to the list of applicable transaction data items
     */
    suspend fun parse(
        transactionData: JsonElement
    ): Map<String, List<TransactionData>> {
        transactionData as? JsonArray
            ?: throw IllegalArgumentException("Invalid transaction_data")
        return parse(transactionData.map { it.jsonPrimitive.content })
    }

    /**
     * Parses OpenID4VP JSON-encoded transaction data.
     *
     * @param transactionData encoded transaction data (array of base64url-encoded items)
     * @param hashAlgorithmOverrides map of credential id to the hash algorithm that must be
     *  used for that specific credential
     * @return map of credential id to the list of applicable transaction data items
     */
    suspend fun parse(
        transactionData: List<String>,
        hashAlgorithmOverrides: Map<String, Algorithm?>? = null
    ): Map<String, List<TransactionData>> {
        val map = mutableMapOf<String, MutableList<TransactionData>>()
        for (encoded in transactionData) {
            val jsonText = encoded.fromBase64Url().decodeToString()
            val data = Json.parseToJsonElement(jsonText).jsonObject
            val credentialIds = data["credential_ids"]!!.jsonArray
            val type = data["type"]!!.jsonPrimitive.content
            val hashAlgorithm = if (hashAlgorithmOverrides != null) {
                // hashAlgorithmOverrides is not null when parsing for verification. At that
                // point the presenter picked an algorithm, we must use that same algorithm.
                hashAlgorithmOverrides[credentialIds.first().jsonPrimitive.content]
            } else {
                data["transaction_data_hashes_alg"]?.let { algs ->
                    algs.jsonArray.firstNotNullOf { alg ->
                        (alg as? JsonPrimitive)?.let {
                            try {
                                Algorithm.fromHashAlgorithmIdentifier(it.content)
                            } catch (_: IllegalArgumentException) {
                                null
                            }
                        }
                    }
                }
            }
            val hash = Crypto.digest(hashAlgorithm ?: Algorithm.SHA256, encoded.encodeToByteArray())
            val parsed = TransactionData(
                hash = ByteString(hash),
                hashAlgorithm = hashAlgorithm,
                type = type,
                jsonData = data
            )
            for (id in credentialIds) {
                map.getOrPut(id.jsonPrimitive.content) { mutableListOf() }.add(parsed)
            }
        }
        return map.mapValues { (_, list) -> list.toList() }
    }
}
