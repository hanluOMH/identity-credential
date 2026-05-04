package org.multipaz.brewery.server

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.multipaz.rpc.backend.BackendEnvironment
import org.multipaz.rpc.handler.InvalidRequestException
import org.multipaz.server.common.getBaseUrl
import org.multipaz.util.Logger
import org.multipaz.verifier.customization.VerifierAssistant
import org.multipaz.verifier.customization.VerifierPresentment
import kotlin.time.Clock

private const val TAG = "BreweryHandler"

// ---------------------------------------------------------------------------
// /brewery/checkout handler
// ---------------------------------------------------------------------------

/**
 * Receives `{productName, price}` and returns `{dcql, transaction_data}` for the browser
 * to pass directly to `multipazVerifyCredentials()`.
 */
suspend fun breweryCheckout(call: ApplicationCall) {
    val body = Json.parseToJsonElement(call.receiveText()).jsonObject
    val productName = body["productName"]?.jsonPrimitive?.contentOrNull
        ?: throw InvalidRequestException("'productName' is missing")
    val price = body["price"]?.jsonPrimitive?.contentOrNull
        ?: throw InvalidRequestException("'price' is missing")

    val transactionData = buildJsonArray {
        add(buildJsonObject {
            put("type", "org.multipaz.transaction.brewery.purchase")
            put("credential_ids", buildJsonArray { add(js("payment")) })
            put("merchant", "Utopia Brewery")
            put("description", productName)
            put("amount", price)
            put("currency", "USD")
        })
    }

    val responsePayload = buildJsonObject {
        put("dcql", buildBreweryDcqlQuery())
        put("protocols", buildJsonArray { add(JsonPrimitive("openid4vp-v1-signed")) })
        put("transaction_data", transactionData)
    }
    call.respondText(responsePayload.toString(), ContentType.Application.Json)
}

// ---------------------------------------------------------------------------
// DCQL query builder
// ---------------------------------------------------------------------------

/** Wraps a String as a JsonPrimitive for use inside buildJsonArray { add(...) }. */
private fun js(s: String): JsonPrimitive = JsonPrimitive(s)

/** Builds a two-element JSON array representing a DCQL claim path: [namespace, element]. */
private fun path(ns: String, elem: String) = buildJsonArray {
    add(js(ns)); add(js(elem))
}

/** Builds a DCQL claim object with an id and a namespace/element path. */
private fun claim(id: String, ns: String, elem: String) = buildJsonObject {
    put("id", id); put("path", path(ns, elem))
}

/** Builds a DCQL claim object with only a namespace/element path (no id). */
private fun claimPath(ns: String, elem: String) = buildJsonObject {
    put("path", path(ns, elem))
}

private fun buildBreweryDcqlQuery(): JsonObject = buildJsonObject {
    put("credentials", buildJsonArray {
        // PhotoID — org.iso.23220.photoid.1
        add(buildJsonObject {
            put("id", "photoid")
            put("format", "mso_mdoc")
            put("meta", buildJsonObject { put("doctype_value", "org.iso.23220.photoid.1") })
            put("claims", buildJsonArray {
                add(claim("given_name",   "org.iso.23220.1",        "given_name"))
                add(claim("family_name",  "org.iso.23220.1",        "family_name"))
                add(claim("birth_date",   "org.iso.23220.1",        "birth_date"))
                add(claim("age_in_years", "org.iso.23220.1",        "age_in_years"))
                add(claim("age_over_18",  "org.iso.23220.photoid.1","age_over_18"))
            })
            put("claim_sets", buildJsonArray {
                add(buildJsonArray { add(js("age_over_18")) })
                add(buildJsonArray { add(js("age_in_years")) })
                add(buildJsonArray { add(js("birth_date")) })
            })
        })
        // mDL — org.iso.18013.5.1.mDL
        add(buildJsonObject {
            put("id", "mdl")
            put("format", "mso_mdoc")
            put("meta", buildJsonObject { put("doctype_value", "org.iso.18013.5.1.mDL") })
            put("claims", buildJsonArray {
                add(claim("given_name",   "org.iso.18013.5.1", "given_name"))
                add(claim("family_name",  "org.iso.18013.5.1", "family_name"))
                add(claim("birth_date",   "org.iso.18013.5.1", "birth_date"))
                add(claim("age_in_years", "org.iso.18013.5.1", "age_in_years"))
                add(claim("age_over_21",  "org.iso.18013.5.1", "age_over_21"))
                add(claim("age_over_18",  "org.iso.18013.5.1", "age_over_18"))
            })
            put("claim_sets", buildJsonArray {
                add(buildJsonArray { add(js("age_over_18")) })
                add(buildJsonArray { add(js("age_over_21")) })
                add(buildJsonArray { add(js("age_in_years")) })
                add(buildJsonArray { add(js("birth_date")) })
            })
        })
        // EU PID — eu.europa.ec.eudi.pid.1
        add(buildJsonObject {
            put("id", "eupid")
            put("format", "mso_mdoc")
            put("meta", buildJsonObject { put("doctype_value", "eu.europa.ec.eudi.pid.1") })
            put("claims", buildJsonArray {
                add(claim("given_name",   "eu.europa.ec.eudi.pid.1", "given_name"))
                add(claim("family_name",  "eu.europa.ec.eudi.pid.1", "family_name"))
                add(claim("birth_date",   "eu.europa.ec.eudi.pid.1", "birth_date"))
                add(claim("age_in_years", "eu.europa.ec.eudi.pid.1", "age_in_years"))
                add(claim("age_over_21",  "eu.europa.ec.eudi.pid.1", "age_over_21"))
                add(claim("age_over_18",  "eu.europa.ec.eudi.pid.1", "age_over_18"))
            })
            put("claim_sets", buildJsonArray {
                add(buildJsonArray { add(js("age_over_18")) })
                add(buildJsonArray { add(js("age_over_21")) })
                add(buildJsonArray { add(js("age_in_years")) })
                add(buildJsonArray { add(js("birth_date")) })
            })
        })
        // Aadhaar — in.gov.uidai.aadhaar.1
        add(buildJsonObject {
            put("id", "aadhaar")
            put("format", "mso_mdoc")
            put("meta", buildJsonObject { put("doctype_value", "in.gov.uidai.aadhaar.1") })
            put("claims", buildJsonArray {
                add(claim("resident_name", "in.gov.uidai.aadhaar.1", "resident_name"))
                add(claim("age_above18",   "in.gov.uidai.aadhaar.1", "age_above18"))
                add(claim("birth_date",    "in.gov.uidai.aadhaar.1", "birth_date"))
            })
            put("claim_sets", buildJsonArray {
                add(buildJsonArray { add(js("age_above18")) })
                add(buildJsonArray { add(js("birth_date")) })
            })
        })
        // Digital Payment Credential — org.multipaz.payment.sca.1
        add(buildJsonObject {
            put("id", "payment")
            put("format", "mso_mdoc")
            put("meta", buildJsonObject { put("doctype_value", "org.multipaz.payment.sca.1") })
            put("claims", buildJsonArray {
                add(claimPath("org.multipaz.payment.sca.1", "issuer_name"))
                add(claimPath("org.multipaz.payment.sca.1", "masked_account_reference"))
                add(claimPath("org.multipaz.payment.sca.1", "holder_name"))
                add(claimPath("org.multipaz.payment.sca.1", "expiry_date"))
            })
        })
    })
    // Require one ID credential AND the payment credential
    put("credential_sets", buildJsonArray {
        add(buildJsonObject {
            put("purpose", "Age verification for alcohol purchase")
            put("options", buildJsonArray {
                add(buildJsonArray { add(js("photoid")) })
                add(buildJsonArray { add(js("mdl")) })
                add(buildJsonArray { add(js("eupid")) })
                add(buildJsonArray { add(js("aadhaar")) })
            })
        })
        add(buildJsonObject {
            put("purpose", "Payment")
            put("options", buildJsonArray {
                add(buildJsonArray { add(js("payment")) })
            })
        })
    })
}

// ---------------------------------------------------------------------------
// VerifierAssistant — runs business logic after credential verification
// ---------------------------------------------------------------------------

class BreweryVerifierAssistant : VerifierAssistant {
    /** Accept all requests unchanged. */
    override suspend fun processRequest(request: JsonObject): VerifierAssistant.ExpandedRequest? = null

    /**
     * After credential verification succeeds, check age and DPC validity.
     * Returns a custom JsonObject with `approved`, `holderName`, `issuerName`, and optionally `error`.
     */
    override suspend fun processResponse(presentment: VerifierPresentment): JsonObject? {
        val response = presentment.response
        Logger.i(TAG, "Credential response keys: ${response.keys}")
        Logger.i(TAG, "Credential response: $response")

        // Find which ID credential was presented
        val (idKey, idClaims) = findIdClaims(response)
            ?: return buildJsonObject {
                put("approved", false)
                put("error", "No recognized identity credential was presented")
            }

        // Age verification
        if (!checkAge(idClaims)) {
            Logger.i(TAG, "Age check failed for credential '$idKey'")
            return buildJsonObject {
                put("approved", false)
                put("error", "Age verification failed: must be 18 or older to purchase alcohol")
            }
        }

        // DPC verification
        val paymentEntry = response["payment"]?.jsonObject
        if (paymentEntry == null) {
            Logger.i(TAG, "DPC check failed: no payment credential in response")
            return buildJsonObject {
                put("approved", false)
                put("error", "No payment credential was presented. Please issue a Digital Payment Credential (DPC) to your wallet first.")
            }
        }
        val paymentClaims = paymentEntry["claims"]?.jsonObject
        val holderName = paymentClaims?.get("holder_name")?.jsonPrimitive?.contentOrNull
        val issuerName = paymentClaims?.get("issuer_name")?.jsonPrimitive?.contentOrNull

        if (holderName.isNullOrBlank() || issuerName.isNullOrBlank()) {
            Logger.i(TAG, "DPC check failed: holder_name='$holderName' issuer_name='$issuerName'")
            return buildJsonObject {
                put("approved", false)
                put("error", "Payment credential is missing required fields (holder_name or issuer_name).")
            }
        }

        // Stub: notify records server (fire-and-forget; errors are logged)
        postToRecordsServer(holderName, issuerName)

        Logger.i(TAG, "Purchase approved for $holderName via $issuerName")
        return buildJsonObject {
            put("approved", true)
            put("holderName", holderName)
            put("issuerName", issuerName)
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Returns the first recognized ID credential's key and its claims object,
 * or null if none is found.
 */
private fun findIdClaims(response: JsonObject): Pair<String, JsonObject>? {
    for (id in listOf("photoid", "mdl", "eupid", "aadhaar")) {
        val claims = response[id]?.jsonObject?.get("claims")?.jsonObject
        if (claims != null) return id to claims
    }
    return null
}

/**
 * Returns true if the age claims indicate the holder is 18 or older.
 *
 * Priority:
 * 1. age_over_18 / age_above18 (Boolean) — definitive 18+ check
 * 2. age_over_21 (Boolean) — true implies 18+; false does NOT mean under 18, so only used as a
 *    positive signal when no age_over_18 claim is present
 * 3. age_in_years (Integer >= 18)
 * 4. birth_date (ISO date string)
 */
fun checkAge(claims: JsonObject): Boolean {
    // Definitive 18+ flags — check these first per the verification flow spec
    claims["age_over_18"]?.jsonPrimitive?.booleanOrNull?.let { return it }
    claims["age_above18"]?.jsonPrimitive?.booleanOrNull?.let { return it }

    // age_over_21 = true implies 18+, but age_over_21 = false does NOT mean under 18
    // (e.g. a 20-year-old has age_over_21=false yet is still over 18)
    claims["age_over_21"]?.jsonPrimitive?.booleanOrNull?.let { if (it) return true }

    // Integer age
    claims["age_in_years"]?.jsonPrimitive?.intOrNull?.let { return it >= 18 }

    // Birth date fallback
    claims["birth_date"]?.jsonPrimitive?.contentOrNull?.let { dateStr ->
        return try {
            val birthDate = LocalDate.parse(dateStr)
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val age = today.year - birthDate.year -
                if (today.month < birthDate.month ||
                    (today.month == birthDate.month && today.day < birthDate.day)
                ) 1 else 0
            age >= 18
        } catch (e: Exception) {
            Logger.e(TAG, "Could not parse birth_date '$dateStr'", e)
            false
        }
    }

    // No usable age claim found
    return false
}

/**
 * Stub: POST purchase info to the records server.
 * The OOB interface is not yet available; errors are logged and swallowed.
 */
private suspend fun postToRecordsServer(holderName: String, issuerName: String) {
    try {
        val recordsUrl = BackendEnvironment.getBaseUrl() + "/records/purchase"
        Logger.i(TAG, "Stub: would POST purchase to $recordsUrl for $holderName via $issuerName")
        // TODO: implement actual HTTP POST once the records server OOB interface is available
    } catch (e: Exception) {
        Logger.e(TAG, "Records server stub call failed", e)
    }
}
