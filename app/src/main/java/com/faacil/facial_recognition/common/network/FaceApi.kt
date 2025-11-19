package com.faacil.facial_recognition.common.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FaceApi {
    @Multipart
    @POST("register")
    suspend fun register(@Part file: MultipartBody.Part): Response<ResponseBody>

    @Multipart
    @POST("login")
    suspend fun login(@Part file: MultipartBody.Part): Response<ResponseBody>
}
