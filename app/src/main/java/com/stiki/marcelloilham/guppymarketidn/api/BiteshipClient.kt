package com.stiki.marcelloilham.guppymarketidn.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BiteshipClient {
    private const val BASE_URL = "https://api.biteship.com/"

    // Live API Key provided by user
    const val API_KEY = "biteship_live.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiR3VwcHkgTWFya2V0IiwidXNlcklkIjoiNmIwNGU2YmUwMjQ2OWI3MTdkYTMwNDk4IiwiaWF0IjoxNzk1NDgzOTgzfQ.ZOcVGOVYKYLUB6cppD5hBWjPBUgJdcZx1jCuROxtU5I"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: BiteshipApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiteshipApi::class.java)
    }
}
