package org.multipaz.presentment

import io.ktor.utils.io.core.toByteArray
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.util.Logger

class TransactionDataCbor(
    hash: ByteString,
    type: String,
    val data: CborMap
): TransactionData(hash, null, type) {
    companion object {
        /**
         * Parses CBOR-encoded transaction data.
         *
         * @param transactionData encoded transaction data
         * @return map of credential index to the list of applicable transaction data items
         */
        suspend fun parse(
            transactionData: List<Bstr>
        ): Map<Int, List<TransactionDataCbor>> {
            val map = mutableMapOf<Int, MutableList<TransactionDataCbor>>()
            for (encoded in transactionData) {
                val data = Cbor.decode(encoded.asBstr) as CborMap
                val credentialIndices = data["credential_ids"].asArray
                val type = data["type"].asTstr
                val hash = Crypto.digest(Algorithm.SHA256, encoded.asBstr)
                val parsed = TransactionDataCbor(ByteString(hash), type, data)
                for (id in credentialIndices) {
                    map.getOrPut(id.asNumber.toInt()) { mutableListOf() }.add(parsed)
                }
            }
            return map.mapValues { (_, list) -> list.toList() }
        }
    }
}