package org.multipaz.presentment

/**
 * A repository for [TransactionDataType] metadata and consent behavior.
 */
class TransactionDataTypeRepository {
    private val _transactionDataTypes: MutableList<TransactionDataType> = mutableListOf()

    /**
     * Get all transaction data types currently registered.
     */
    val transactionDataTypes: List<TransactionDataType>
        get() = _transactionDataTypes

    /**
     * Registers a [TransactionDataType].
     *
     * @param transactionDataType the type metadata/behavior to register.
     */
    fun addTransactionDataType(transactionDataType: TransactionDataType) =
        _transactionDataTypes.add(transactionDataType)

    /**
     * Looks up a registered transaction data type by `type`.
     *
     * @param type transaction data type identifier.
     * @return the matching [TransactionDataType] or `null`.
     */
    fun getTransactionDataType(type: String): TransactionDataType? =
        _transactionDataTypes.find { it.type == type }

    /**
     * Builds consent content for a transaction data item.
     *
     * If a matching [TransactionDataType] is registered, its custom consent model is used.
     * Otherwise this falls back to a generic type-only consent model.
     *
     * @param transactionData transaction data item.
     * @return consent content model for UI rendering.
     */
    fun getConsentModel(transactionData: TransactionData): TransactionDataConsentModel {
        val registeredType = getTransactionDataType(transactionData.type)
        if (registeredType != null) {
            return registeredType.createConsentModel(transactionData)
        }
        return TransactionDataConsentModel(
            title = "Transaction Data",
            summary = "Transaction type: ${transactionData.type}",
            fields = listOf("Type" to transactionData.type)
        )
    }
}
