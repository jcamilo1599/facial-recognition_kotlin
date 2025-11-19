package com.faacil.facial_recognition.common.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Definición de endpoints del backend de reconocimiento facial.
 *
 * Ambos endpoints esperan un archivo de imagen (PNG/JPEG) enviado como multipart con el campo `file`.
 * Se devuelve [Response<ResponseBody>] para poder inspeccionar:
 * - Código de estado HTTP (200, 409, 503, ...)
 * - Cuerpo literal de texto/JSON para mostrarlo en la UI sin parseo adicional.
 */
interface FaceApi {
    @Multipart
    @POST("register")
    suspend fun register(@Part file: MultipartBody.Part): Response<ResponseBody>

    @Multipart
    @POST("login")
    suspend fun login(@Part file: MultipartBody.Part): Response<ResponseBody>
}
