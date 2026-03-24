package org.multipaz.verifier.server

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.server.common.runServer
import org.multipaz.util.Logger
import org.multipaz.verifier.transaction.TransactionProcessor

/**
 * Main entry point to launch the server.
 *
 * Build and start the server using
 *
 * ```./gradlew multipaz-verifier-server:run```
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runServer(args, environmentInitializer = {
                add(TransactionProcessor::class, object: TransactionProcessor {
                    override suspend fun checkRequest(
                        dcql: JsonObject,
                        transactionData: JsonObject
                    ) {
                        // accept all transactions for testing
                    }

                    override suspend fun processResponse(
                        dcql: JsonObject,
                        transactionData: JsonObject,
                        responseProtocol: String,
                        response: ByteString,
                        result: JsonObject
                    ) {
                        // no actual processing
                        Logger.i("TransactionProcessor",
                            "Got transaction data: $transactionData")
                    }
                })
            }) { environment ->
                configureRouting(environment)
            }
        }
    }
}