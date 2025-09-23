package com.example.echoplay_frontend.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    //https://downloaded-warranty-skill-common.trycloudflare.com/ http://192.168.1.35:3000/
    private const val BASE_URL = "http://192.168.1.35:3000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}