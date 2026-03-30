package org.multipaz.transactiondata.knowntypes

import org.multipaz.credential.Credential
import org.multipaz.documenttype.TransactionType
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.presentment.TransactionData

/**
 * [TransactionType] for [UtopiaPaymentTransactionType] JSON payloads (OpenID4VP / DCQL).
 *
 * Must be registered on [org.multipaz.documenttype.DocumentTypeRepository] so credential matching
 * can evaluate transaction applicability. The sample payment mdoc doc type is
 * `org.multipaz.payment.sca.1` ([org.multipaz.documenttype.knowntypes.DigitalPaymentCredential] in multipaz-doctypes).
 */
object UtopiaPaymentAuthorizationTransaction : TransactionType(
    displayName = "Utopia payment authorization",
    identifier = UtopiaPaymentTransactionType.TYPE,
    attributes = emptyList(),
) {
    /** Matches [org.multipaz.documenttype.knowntypes.DigitalPaymentCredential.CARD_DOCTYPE]. */
    private const val PAYMENT_SCA_DOCTYPE = "org.multipaz.payment.sca.1"

    override suspend fun isApplicable(
        transactionData: TransactionData,
        credential: Credential
    ): Boolean =
        credential is MdocCredential && credential.docType == PAYMENT_SCA_DOCTYPE
}
