package org.multipaz.presentment

/**
 * Type metadata and consent rendering contract for [TransactionData].
 */
interface TransactionDataType {
    /**
     * Type identifier, matching [TransactionData.type].
     */
    val type: String

    /**
     * Human-readable name for the transaction type.
     */
    val displayName: String

    /**
     * Creates consent content for a transaction data item of this [type].
     */
    fun createConsentModel(transactionData: TransactionData): TransactionDataConsentModel
}

