package org.multipaz.transactiondata.knowntypes

import org.multipaz.presentment.TransactionDataTypeRepository

/**
 * Registration helpers for Utopia transaction-data types.
 */
object UtopiaTransactionDataTypes {
    /**
     * Registers default Utopia transaction-data types into a repository.
     */
    fun registerDefaultTypes(
        transactionDataTypeRepository: TransactionDataTypeRepository
    ): TransactionDataTypeRepository {
        transactionDataTypeRepository.addTransactionDataType(UtopiaPaymentTransactionType)
        return transactionDataTypeRepository
    }
}

