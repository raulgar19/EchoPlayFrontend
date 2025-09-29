package com.example.echoplay_frontend.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    //https://downloaded-warranty-skill-common.trycloudflare.com/ http://192.168.1.35:3000/   parcela http://192.168.68.114:3000/
    private const val BASE_URL = "https://downloaded-warranty-skill-common.trycloudflare.com/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}