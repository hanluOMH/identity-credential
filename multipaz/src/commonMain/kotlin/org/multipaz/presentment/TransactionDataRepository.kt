package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString

/**
 * A repository for [TransactionData] items.
 *
 * The repository is initially empty. Applications may add transaction data as part of request
 * processing or when preparing presentment state.
 */
class TransactionDataRepository {
    private val _transactionData: MutableList<TransactionData> = mutableListOf()

    /**
     * Get all transaction data currently in the repository.
     */
    val transactionData: List<TransactionData>
        get() = _transactionData

    /**
     * Add a [TransactionData] item.
     *
     * @param item the transaction data item to add.
     */
    fun addTransactionData(item: TransactionData) =
        _transactionData.add(item)

    /**
     * Gets all transaction data items of a given `type`.
     *
     * @param type the transaction data type.
     * @return a list of matching items (possibly empty).
     */
    fun getTransactionData(type: String): List<TransactionData> =
        _transactionData.filter { it.type == type }

    /**
     * Gets a transaction data item by hash.
     *
     * @param hash transaction data hash.
     * @return the first matching item or `null`.
     */
    fun getTransactionData(hash: ByteString): TransactionData? =
        _transactionData.find { it.hash == hash }
}

