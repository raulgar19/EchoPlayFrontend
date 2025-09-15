package com.example.echoplay_frontend.data.repository

import com.example.echoplay_frontend.data.models.User
import com.example.echoplay_frontend.data.network.RetrofitInstance

class UserRepository {
    private val api = RetrofitInstance.api

    suspend fun getUsers(): List<User> {
        return api.getUsers()
    }
}
