package org.multipaz.provision.openid4vci

import org.multipaz.crypto.Algorithm

data class ClientPreferences(
    val clientId: String,
    val redirectUrl: String,
    val locales: List<String>,
    val signingAlgorithms: List<Algorithm>
)