package com.faacil.facial_recognition.common.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton encargado de la configuración y creación de la instancia de [Retrofit].
 *
 * Este objeto centraliza la configuración HTTP para la comunicación con el servicio de
 * reconocimiento facial.
 *
 * Características principales:
 * - Logging para depuración detallada (request/response).
 * - Timeouts: Configuración conservadora (20s/30s) optimizada para subida de imágenes.
 * - Headers: Inyección automática de `Accept: application/json`.
 * - Serialización: Uso de Moshi para el parseo de JSON.
 */
object ApiClient {

    /**
     * URL base del servicio de reconocimiento facial.
     *
     * @note Considere mover esta constante a `BuildConfig` para gestionar entornos (Dev/Prod).
     */
    private const val BASE_URL = "https://facial-recognition-api-215011024799.us-central1.run.app/"

    /**
     * Instancia configurada de [OkHttpClient] con carga perezosa (lazy).
     */
    private val okHttp: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            // Level.BODY expone información sensible en los logs.
            // Usar Level.BASIC o Level.NONE en versiones de producción.
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("Accept", "application/json")
                    .build()

                chain.proceed(requestWithHeaders)
            }
            .addInterceptor(logger)
            .build()
    }

    /**
     * Instancia pública de [Retrofit] lista para crear servicios de API.
     *
     * Utiliza [MoshiConverterFactory] para la conversión de datos y el cliente [okHttp]
     * configurado internamente.
     */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttp)
            .build()
    }
}