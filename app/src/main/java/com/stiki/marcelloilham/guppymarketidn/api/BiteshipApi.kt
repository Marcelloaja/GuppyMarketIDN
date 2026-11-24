package com.stiki.marcelloilham.guppymarketidn.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface BiteshipApi {

    @POST("v1/rates/couriers")
    fun getShippingRates(
        @Header("Authorization") authorization: String,
        @Body request: RatesRequest
    ): Call<RatesResponse>

    @GET("v1/trackings/{waybill_id}/couriers/{courier_code}")
    fun getTracking(
        @Header("Authorization") authorization: String,
        @Path("waybill_id") waybillId: String,
        @Path("courier_code") courierCode: String
    ): Call<TrackingResponse>
}
