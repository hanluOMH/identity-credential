package org.multipaz.presentment

/**
 * Global registry for transaction-data type rendering used by consent UIs.
 */
object TransactionDataTypeRegistry {
    private var repository: TransactionDataTypeRepository = TransactionDataTypeRepository()

    /**
     * Sets the active transaction-data type repository.
     */
    fun setRepository(transactionDataTypeRepository: TransactionDataTypeRepository) {
        repository = transactionDataTypeRepository
    }

    /**
     * Gets the active transaction-data type repository.
     */
    fun getRepository(): TransactionDataTypeRepository = repository

    /**
     * Resolves consent model for a transaction data item using the active repository.
     */
    fun getConsentModel(transactionData: TransactionData): TransactionDataConsentModel =
        repository.getConsentModel(transactionData)
}

