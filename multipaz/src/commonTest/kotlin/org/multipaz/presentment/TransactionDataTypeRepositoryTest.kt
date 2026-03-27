package org.multipaz.presentment

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransactionDataTypeRepositoryTest {
    private object TestType: TransactionDataType {
        override val type: String = "org.multipaz.test.tx-type"
        override val displayName: String = "Test Transaction Type"

        override fun createConsentModel(transactionData: TransactionData): TransactionDataConsentModel {
            return TransactionDataConsentModel(
                title = displayName,
                summary = "Test summary",
                fields = listOf("Type" to transactionData.type)
            )
        }
    }

    @Test
    fun addAndLookupType() {
        val repository = TransactionDataTypeRepository()
        repository.addTransactionDataType(TestType)

        val lookedUp = repository.getTransactionDataType(TestType.type)
        assertNotNull(lookedUp)
        assertEquals(TestType, lookedUp)
        assertEquals(1, repository.transactionDataTypes.size)
        assertNull(repository.getTransactionDataType("does.not.exist"))
    }

    @Test
    fun consentModelDelegatesToType() {
        val transactionData = TransactionData(
            hash = ByteString(byteArrayOf(1)),
            hashAlgorithm = null,
            type = TestType.type
        )

        val model = TestType.createConsentModel(transactionData)

        assertEquals("Test Transaction Type", model.title)
        assertEquals("Test summary", model.summary)
        assertEquals(listOf("Type" to TestType.type), model.fields)
    }

    @Test
    fun getConsentModel_fallsBackToGenericTypeOnly() {
        val repository = TransactionDataTypeRepository()
        val transactionData = TransactionData(
            hash = ByteString(byteArrayOf(1)),
            hashAlgorithm = null,
            type = "org.multipaz.unknown.transaction"
        )

        val model = repository.getConsentModel(transactionData)

        assertEquals("Transaction Data", model.title)
        assertEquals("Transaction type: org.multipaz.unknown.transaction", model.summary)
        assertEquals(listOf("Type" to "org.multipaz.unknown.transaction"), model.fields)
    }

    @Test
    fun getConsentModel_usesRegisteredTypeModel() {
        val repository = TransactionDataTypeRepository()
        repository.addTransactionDataType(TestType)
        val transactionData = TransactionData(
            hash = ByteString(byteArrayOf(2)),
            hashAlgorithm = null,
            type = TestType.type
        )

        val model = repository.getConsentModel(transactionData)

        assertEquals("Test Transaction Type", model.title)
        assertEquals("Test summary", model.summary)
        assertEquals(listOf("Type" to TestType.type), model.fields)
    }
}
