package com.example.securescan.data.remote

import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.data.model.UrlScanResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the VirusTotal v3 API.
 */
interface VirusTotalApi {

    /**
     * Submits a URL for analysis.
     */
    @FormUrlEncoded
    @POST("urls")
    suspend fun scanUrl(
        @Header("x-apikey") apiKey: String,
        @Field("url") url: String
    ): Response<UrlScanResponse>

    /**
     * Retrieves the results of a URL or file analysis.
     */
    @GET("analyses/{id}")
    suspend fun getAnalysis(
        @Header("x-apikey") apiKey: String,
        @Path("id") id: String
    ): Response<AnalysisResponse>

    /**
     * Uploads a file for analysis.
     */
    @Multipart
    @POST("files")
    suspend fun uploadFile(
        @Header("x-apikey") apiKey: String,
        @Part file: MultipartBody.Part
    ): Response<UrlScanResponse>
}