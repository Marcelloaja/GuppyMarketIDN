package com.stiki.marcelloilham.guppymarketidn.api

import com.google.gson.annotations.SerializedName

// 1. RATES / CEK ONGKIR REQUEST
data class RatesRequest(
    @SerializedName("origin_latitude") val originLatitude: Double,
    @SerializedName("origin_longitude") val originLongitude: Double,
    @SerializedName("destination_latitude") val destinationLatitude: Double,
    @SerializedName("destination_longitude") val destinationLongitude: Double,
    @SerializedName("couriers") val couriers: String = "jnt,jne,sicepat",
    @SerializedName("items") val items: List<RateItem>
)

data class RateItem(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: Long,
    @SerializedName("weight") val weight: Int // in grams
)

// RATES / CEK ONGKIR RESPONSE
data class RatesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("pricing") val pricing: List<PricingOption>?
)

data class PricingOption(
    @SerializedName("available_for_cash_on_delivery") val codAvailable: Boolean,
    @SerializedName("courier_name") val courierName: String,
    @SerializedName("courier_code") val courierCode: String,
    @SerializedName("courier_service_name") val courierServiceName: String,
    @SerializedName("courier_service_code") val courierServiceCode: String,
    @SerializedName("duration") val duration: String?,
    @SerializedName("price") val price: Long,
    @SerializedName("type") val type: String?
)

// 2. TRACKING / LACAK RESI RESPONSE
data class TrackingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("waybill_id") val waybillId: String?,
    @SerializedName("courier") val courier: CourierInfo?,
    @SerializedName("history") val history: List<TrackingHistory>?
)

data class CourierInfo(
    @SerializedName("company") val company: String,
    @SerializedName("name") val name: String
)

data class TrackingHistory(
    @SerializedName("note") val note: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("status") val status: String
)
