package org.multipaz.transactiondata.knowntypes

import org.multipaz.documenttype.DocumentTypeRepository
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

    /**
     * Registers [UtopiaPaymentAuthorizationTransaction] for DCQL/OpenID4VP transaction matching.
     */
    fun registerPaymentAuthorizationTransactionType(
        documentTypeRepository: DocumentTypeRepository
    ): DocumentTypeRepository {
        documentTypeRepository.addTransactionType(UtopiaPaymentAuthorizationTransaction)
        return documentTypeRepository
    }
}

