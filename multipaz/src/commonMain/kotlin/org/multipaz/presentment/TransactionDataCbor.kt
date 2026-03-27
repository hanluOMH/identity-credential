package org.multipaz.presentment

import io.ktor.utils.io.core.toByteArray
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto

/**
 * Parser utilities for CBOR-encoded ISO mdoc transaction data.
 */
object TransactionDataCbor {
    /**
     * Parses CBOR-encoded transaction data.
     *
     * @param transactionData encoded transaction data
     * @return map of credential index to the list of applicable transaction data items
     */
    suspend fun parse(
        transactionData: List<Bstr>
    ): Map<Int, List<TransactionData>> {
        val map = mutableMapOf<Int, MutableList<TransactionData>>()
        for (encoded in transactionData) {
            val data = Cbor.decode(encoded.asBstr) as CborMap
            val credentialIndices = data["credential_ids"].asArray
            val type = data["type"].asTstr
            val hash = Crypto.digest(Algorithm.SHA256, encoded.asBstr)
            val parsed = TransactionData(
                hash = ByteString(hash),
                hashAlgorithm = null,
                type = type,
                cborData = data
            )
            for (id in credentialIndices) {
                map.getOrPut(id.asNumber.toInt()) { mutableListOf() }.add(parsed)
            }
        }
        return map.mapValues { (_, list) -> list.toList() }
    }
}
