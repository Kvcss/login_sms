package com.login.app.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

// Endereco do servidor.
//   - Emulador Android -> host da maquina = 10.0.2.2
//   - Dispositivo fisico -> troque pelo IP da maquina na rede (ex.: 192.168.0.10)
const val BASE_URL = "http://10.0.2.2:3000/"

interface LoginApi {
    // login e confirm podem retornar 200 (UserResponse) ou outros codigos
    // (202/404/400) com corpos diferentes, entao tratamos a Response crua.
    @POST("users/login")
    suspend fun login(@Body body: LoginRequest): Response<ResponseBody>

    @POST("users/confirm")
    suspend fun confirm(@Body body: ConfirmRequest): Response<ResponseBody>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body body: UpdateUserRequest): Response<UserResponse>
}

object Network {
    private val json = Json { ignoreUnknownKeys = true }

    val api: LoginApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LoginApi::class.java)
    }

    val jsonParser: Json get() = json
}
