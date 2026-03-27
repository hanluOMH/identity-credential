package org.multipaz.presentment

/**
 * Content model for presenting transaction data in consent UI.
 *
 * @param title short title shown to the user.
 * @param summary short summary line shown under the title.
 * @param fields ordered list of key/value rows for display.
 */
data class TransactionDataConsentModel(
    val title: String,
    val summary: String,
    val fields: List<Pair<String, String>> = emptyList()
)

