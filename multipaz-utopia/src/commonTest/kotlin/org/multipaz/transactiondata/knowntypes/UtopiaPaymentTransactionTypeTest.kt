package org.multipaz.transactiondata.knowntypes

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.presentment.TransactionDataJson
import org.multipaz.presentment.TransactionDataRepository
import org.multipaz.presentment.TransactionDataTypeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UtopiaPaymentTransactionTypeTest {
    @Test
    fun createTransactionData_roundTripsThroughParser() = runTest {
        val request = UtopiaPaymentTransactionType.PaymentRequest(
            credentialIds = listOf("cred-1"),
            amountMinor = 1999,
            currency = "USD",
            payee = "Brewery of Utopia",
            paymentReference = "order-42"
        )
        val encoded = UtopiaPaymentTransactionType.createEncodedJsonTransactionData(request)

        val parsed = TransactionDataJson.parse(JsonArray(listOf(JsonPrimitive(encoded))))
        val parsedItem = parsed["cred-1"]?.firstOrNull()

        assertNotNull(parsedItem)
        assertEquals(UtopiaPaymentTransactionType.TYPE, parsedItem.type)
        assertEquals("USD", parsedItem.jsonData?.get("currency")?.toString()?.trim('"'))
        assertEquals(1999L, parsedItem.jsonData?.get("amount_minor")?.toString()?.toLong())
    }

    @Test
    fun addToRepository_addsPaymentTransactionData() = runTest {
        val repository = TransactionDataRepository()
        val request = UtopiaPaymentTransactionType.PaymentRequest(
            credentialIds = listOf("cred-1", "cred-2"),
            amountMinor = 2500,
            currency = "EUR",
            payee = "Bank of Utopia"
        )

        val added = UtopiaPaymentTransactionType.addToRepository(repository, request)

        assertEquals(1, repository.transactionData.size)
        assertEquals(added, repository.transactionData.first())
        assertEquals(UtopiaPaymentTransactionType.TYPE, added.type)
    }

    @Test
    fun createConsentModel_formatsPaymentData() = runTest {
        val request = UtopiaPaymentTransactionType.PaymentRequest(
            credentialIds = listOf("cred-1"),
            amountMinor = 1099,
            currency = "USD",
            payee = "Utopia Bank",
            paymentReference = "inv-7"
        )
        val transactionData = UtopiaPaymentTransactionType.createTransactionData(request)

        val model = UtopiaPaymentTransactionType.createConsentModel(transactionData)

        assertEquals("Utopia Payment Authorization", model.title)
        assertEquals("Authorize payment of 1099 USD to Utopia Bank", model.summary)
        assertEquals("Utopia Bank", model.fields.first { it.first == "Payee" }.second)
        assertEquals("1099", model.fields.first { it.first == "Amount (minor units)" }.second)
        assertEquals("USD", model.fields.first { it.first == "Currency" }.second)
        assertEquals("inv-7", model.fields.first { it.first == "Reference" }.second)
    }

    @Test
    fun registerHelper_registersPaymentType() {
        val typeRepository = TransactionDataTypeRepository()

        UtopiaTransactionDataTypes.registerDefaultTypes(typeRepository)

        val lookedUp = typeRepository.getTransactionDataType(UtopiaPaymentTransactionType.TYPE)
        assertEquals(UtopiaPaymentTransactionType, lookedUp)
    }
}
