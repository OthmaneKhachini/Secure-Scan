package com.example.securescan.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for VirusTotal v3 API responses.
 */

data class AnalysisResponse(
    @SerializedName("data") val data: AnalysisData
)

data class AnalysisData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("links") val links: Links?,
    @SerializedName("attributes") val attributes: AnalysisAttributes?
)

data class Links(
    @SerializedName("self") val self: String,
    @SerializedName("item") val item: String?
)

data class AnalysisAttributes(
    @SerializedName("status") val status: String,
    @SerializedName("stats") val stats: AnalysisStats,
    @SerializedName("results") val results: Map<String, EngineResult>,
    @SerializedName("date") val date: Long?,
    @SerializedName("reputation") val reputation: Int?
)

data class AnalysisStats(
    @SerializedName("harmless") val harmless: Int,
    @SerializedName("type-unsupported") val typeUnsupported: Int,
    @SerializedName("suspicious") val suspicious: Int,
    @SerializedName("confirmed-timeout") val confirmedTimeout: Int,
    @SerializedName("timeout") val timeout: Int,
    @SerializedName("failure") val failure: Int,
    @SerializedName("malicious") val malicious: Int,
    @SerializedName("undetected") val undetected: Int
)

data class EngineResult(
    @SerializedName("method") val method: String,
    @SerializedName("engine_name") val engineName: String,
    @SerializedName("category") val category: String,
    @SerializedName("result") val result: String?,
    @SerializedName("engine_version") val engineVersion: String?,
    @SerializedName("engine_update") val engineUpdate: String?
)

data class UrlScanResponse(
    @SerializedName("data") val data: ScanData
)

data class ScanData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String
)
