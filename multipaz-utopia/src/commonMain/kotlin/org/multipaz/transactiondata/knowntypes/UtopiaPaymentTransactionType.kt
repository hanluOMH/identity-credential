package org.multipaz.transactiondata.knowntypes

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.presentment.TransactionDataConsentModel
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionDataRepository
import org.multipaz.presentment.TransactionDataType
import org.multipaz.util.toBase64Url
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.jsonPrimitive

/**
 * Utopia example transaction-data type for payment authorization.
 *
 * This is an example payload shape and type identifier for a fictional Utopia ecosystem.
 */
object UtopiaPaymentTransactionType: TransactionDataType {
    const val TYPE = "org.multipaz.utopia.payment.v1"
    override val type: String
        get() = TYPE
    override val displayName: String
        get() = "Utopia Payment Authorization"

    data class PaymentRequest(
        val credentialIds: List<String>,
        val amountMinor: Long,
        val currency: String,
        val payee: String,
        val paymentReference: String? = null
    )

    /**
     * Creates the JSON payload for OpenID4VP `transaction_data`.
     */
    fun createJsonPayload(
        paymentRequest: PaymentRequest,
        hashAlgorithm: Algorithm? = null
    ): JsonObject {
        require(paymentRequest.credentialIds.isNotEmpty()) {
            "credentialIds must not be empty"
        }
        require(paymentRequest.currency.isNotBlank()) {
            "currency must not be blank"
        }
        require(paymentRequest.payee.isNotBlank()) {
            "payee must not be blank"
        }
        return buildJsonObject {
            put("type", TYPE)
            putJsonArray("credential_ids") {
                paymentRequest.credentialIds.forEach { add(JsonPrimitive(it)) }
            }
            put("amount_minor", paymentRequest.amountMinor)
            put("currency", paymentRequest.currency)
            put("payee", paymentRequest.payee)
            paymentRequest.paymentReference?.let { reference ->
                put("payment_reference", JsonPrimitive(reference))
            }
            hashAlgorithm?.let { algorithm ->
                putJsonArray("transaction_data_hashes_alg") {
                    add(JsonPrimitive(algorithm.hashAlgorithmName))
                }
            }
        }
    }

    /**
     * Creates base64url-encoded JSON transaction data for OpenID4VP.
     */
    fun createEncodedJsonTransactionData(
        paymentRequest: PaymentRequest,
        hashAlgorithm: Algorithm? = null
    ): String {
        val payload = createJsonPayload(paymentRequest, hashAlgorithm)
        return Json.encodeToString(JsonObject.serializer(), payload)
            .encodeToByteArray()
            .toBase64Url()
    }

    /**
     * Creates [TransactionData] for the payment request.
     */
    suspend fun createTransactionData(
        paymentRequest: PaymentRequest,
        hashAlgorithm: Algorithm? = null
    ): TransactionData {
        val payload = createJsonPayload(paymentRequest, hashAlgorithm)
        val encoded = Json.encodeToString(JsonObject.serializer(), payload)
            .encodeToByteArray()
            .toBase64Url()
        val digest = Crypto.digest(hashAlgorithm ?: Algorithm.SHA256, encoded.encodeToByteArray())
        return TransactionData(
            hash = ByteString(digest),
            hashAlgorithm = hashAlgorithm,
            type = TYPE,
            jsonData = payload
        )
    }

    /**
     * Creates and adds transaction data to a repository.
     */
    suspend fun addToRepository(
        transactionDataRepository: TransactionDataRepository,
        paymentRequest: PaymentRequest,
        hashAlgorithm: Algorithm? = null
    ): TransactionData {
        val transactionData = createTransactionData(paymentRequest, hashAlgorithm)
        transactionDataRepository.addTransactionData(transactionData)
        return transactionData
    }

    override fun createConsentModel(transactionData: TransactionData): TransactionDataConsentModel {
        val jsonData = transactionData.jsonData
        val amountMinor = jsonData?.get("amount_minor")?.jsonPrimitive?.contentOrNull
        val currency = jsonData?.get("currency")?.jsonPrimitive?.contentOrNull
        val payee = jsonData?.get("payee")?.jsonPrimitive?.contentOrNull

        val summary = if (amountMinor != null && currency != null && payee != null) {
            "You are sharing a payment credential with $payee for $amountMinor $currency."
        } else {
            "You are sharing a payment credential."
        }

        return TransactionDataConsentModel(
            title = "Payment credential sharing",
            summary = summary,
            fields = emptyList()
        )
    }
}
