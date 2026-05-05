package org.multipaz.utopia.knowntypes

import kotlinx.datetime.LocalDate
import org.multipaz.cbor.toDataItem
import org.multipaz.cbor.toDataItemFullDate
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.DocumentType
import org.multipaz.documenttype.Icon
import org.multipaz.documenttype.knowntypes.SampleData

/**
 * Demo Bank of Utopia account credential.
 *
 * This document type is intentionally non-normative and exists to verify that Utopia issuer
 * composition can support organization-specific credentials without an issuer superclass.
 */
object BankAccountCredential {
    const val BANK_ACCOUNT_DOCTYPE = "org.multipaz.utopia.bank.account.1"
    const val BANK_ACCOUNT_NAMESPACE = "org.multipaz.utopia.bank.account.1"

    fun getDocumentType(locale: String = "en-US"): DocumentType {
        return DocumentType.Builder("Bank account")
            .addMdocDocumentType(BANK_ACCOUNT_DOCTYPE)
            .addMdocAttribute(
                DocumentAttributeType.String,
                "issuer_name",
                "Issuer name",
                "Name of the bank that issued the credential.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.ACCOUNT_BALANCE,
                "Bank of Utopia".toDataItem()
            )
            .addMdocAttribute(
                DocumentAttributeType.String,
                "account_holder_name",
                "Account holder name",
                "Name of the account holder.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.PERSON,
                "${SampleData.GIVEN_NAME} ${SampleData.FAMILY_NAME}".toDataItem()
            )
            .addMdocAttribute(
                DocumentAttributeType.String,
                "account_id",
                "Account ID",
                "Demo account identifier.",
                false,
                BANK_ACCOUNT_NAMESPACE,
                Icon.NUMBERS,
                "BOU-00001234".toDataItem()
            )
            .addMdocAttribute(
                DocumentAttributeType.String,
                "masked_account_reference",
                "Masked account reference",
                "Masked account reference for display and verification.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.NUMBERS,
                "****1234".toDataItem()
            )
            .addMdocAttribute(
                DocumentAttributeType.String,
                "account_type",
                "Account type",
                "Type of bank account.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.ACCOUNT_BALANCE,
                "checking".toDataItem()
            )
            .addMdocAttribute(
                DocumentAttributeType.Date,
                "issue_date",
                "Issue date",
                "Date the bank account credential was issued.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.CALENDAR_CLOCK,
                LocalDate.parse(SampleData.ISSUE_DATE).toDataItemFullDate()
            )
            .addMdocAttribute(
                DocumentAttributeType.Date,
                "expiry_date",
                "Expiry date",
                "Date the bank account credential expires.",
                true,
                BANK_ACCOUNT_NAMESPACE,
                Icon.CALENDAR_CLOCK,
                LocalDate.parse(SampleData.EXPIRY_DATE).toDataItemFullDate()
            )
            .addSampleRequest(
                id = "bank_account_minimal",
                displayName = "Bank account",
                mdocDataElements = mapOf(
                    BANK_ACCOUNT_NAMESPACE to mapOf(
                        "issuer_name" to false,
                        "account_holder_name" to false,
                        "masked_account_reference" to false,
                        "account_type" to false,
                        "issue_date" to false,
                        "expiry_date" to false,
                    )
                )
            )
            .addSampleRequest(
                id = "bank_account_full",
                displayName = "Bank account full",
                mdocDataElements = mapOf(BANK_ACCOUNT_NAMESPACE to mapOf())
            )
            .build()
    }
}
