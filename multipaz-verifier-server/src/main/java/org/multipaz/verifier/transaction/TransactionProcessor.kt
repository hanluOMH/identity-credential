package org.multipaz.verifier.transaction

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject

interface TransactionProcessor {
    suspend fun checkRequest(
        dcql: JsonObject,
        transactionData: JsonObject
    )
    suspend fun processResponse(
        dcql: JsonObject,
        transactionData: JsonObject,
        responseProtocol: String,
        response: ByteString,
        result: JsonObject
    )
}