package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionDataRepositoryTest {
    @Test
    fun addAndLookupByTypeAndHash() {
        val repository = TransactionDataRepository()
        val tx1 = TransactionData(
            hash = ByteString(byteArrayOf(1, 2, 3)),
            hashAlgorithm = null,
            type = "org.multipaz.transaction.payment"
        )
        val tx2 = TransactionData(
            hash = ByteString(byteArrayOf(4, 5, 6)),
            hashAlgorithm = null,
            type = "org.multipaz.transaction.payment"
        )
        val tx3 = TransactionData(
            hash = ByteString(byteArrayOf(7, 8, 9)),
            hashAlgorithm = null,
            type = "org.multipaz.transaction.document-signing"
        )

        repository.addTransactionData(tx1)
        repository.addTransactionData(tx2)
        repository.addTransactionData(tx3)

        assertEquals(3, repository.transactionData.size)
        assertEquals(listOf(tx1, tx2), repository.getTransactionData("org.multipaz.transaction.payment"))
        assertEquals(listOf(tx3), repository.getTransactionData("org.multipaz.transaction.document-signing"))
        assertEquals(tx2, repository.getTransactionData(ByteString(byteArrayOf(4, 5, 6))))
        assertNull(repository.getTransactionData(ByteString(byteArrayOf(0))))
    }
}

