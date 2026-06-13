package com.example.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SatsWalkApiClient {

    private var apiService: SatsWalkApiService? = null

    fun getService(context: Context): SatsWalkApiService {
        return apiService ?: synchronized(this) {
            val service = buildService(context)
            apiService = service
            service
        }
    }

    private fun buildService(context: Context): SatsWalkApiService {
        // OkHttpClient with our MockServerInterceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MockServerInterceptor(context))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        // Configure Moshi with KotlinJsonAdapterFactory for type-safe parsing
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        // Retrofit builder pointing to our mock cloud endpoint
        return Retrofit.Builder()
            .baseUrl("https://api.satswalk.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SatsWalkApiService::class.java)
    }
}
