package com.faacil.facial_recognition.common.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://facial-recognition-api-215011024799.us-central1.run.app/"

    private val okHttp: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            // Para diagnosticar diferencias con curl, elevamos a BODY.
            // En producción se puede bajar a BASIC.
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Alinear encabezados con curl
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logger)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttp)
            .build()
    }
}
