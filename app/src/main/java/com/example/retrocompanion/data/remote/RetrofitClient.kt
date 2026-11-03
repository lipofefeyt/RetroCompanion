package com.example.retrocompanion.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://retroachievements.org/API/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RedactingLoggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: RetroAchievementsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RetroAchievementsApi::class.java)
    }
}
