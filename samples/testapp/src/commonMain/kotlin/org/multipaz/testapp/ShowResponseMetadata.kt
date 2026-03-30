package org.multipaz.testapp

import org.multipaz.cbor.DataItem
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem

data class ShowResponseMetadata(
    val engagementType: String,
    val transferProtocol: String,
    val requestSize: Long,
    val responseSize: Long,
    val durationMsecNfcTapToEngagement: Long?,
    val durationMsecEngagementReceivedToRequestSent: Long?,
    val durationMsecRequestSentToResponseReceived: Long
) {
    fun toDataItem(): DataItem = buildCborMap {
        put("engagementType", engagementType)
        put("transferProtocol", transferProtocol)
        put("requestSize", requestSize)
        put("responseSize", responseSize)
        durationMsecNfcTapToEngagement?.let { put("durationMsecNfcTapToEngagement", it) }
        durationMsecEngagementReceivedToRequestSent?.let {
            put("durationMsecEngagementReceivedToRequestSent", it)
        }
        put("durationMsecRequestSentToResponseReceived", durationMsecRequestSentToResponseReceived)
    }

    companion object {
        fun fromDataItem(dataItem: DataItem): ShowResponseMetadata = ShowResponseMetadata(
            engagementType = dataItem["engagementType"].asTstr,
            transferProtocol = dataItem["transferProtocol"].asTstr,
            requestSize = dataItem["requestSize"].asNumber,
            responseSize = dataItem["responseSize"].asNumber,
            durationMsecNfcTapToEngagement = if (dataItem.hasKey("durationMsecNfcTapToEngagement")) {
                dataItem["durationMsecNfcTapToEngagement"].asNumber
            } else {
                null
            },
            durationMsecEngagementReceivedToRequestSent =
                if (dataItem.hasKey("durationMsecEngagementReceivedToRequestSent")) {
                    dataItem["durationMsecEngagementReceivedToRequestSent"].asNumber
                } else {
                    null
                },
            durationMsecRequestSentToResponseReceived =
                dataItem["durationMsecRequestSentToResponseReceived"].asNumber
        )
    }
}
