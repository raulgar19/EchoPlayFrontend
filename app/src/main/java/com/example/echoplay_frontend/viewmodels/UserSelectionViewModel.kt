package com.example.echoplay_frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoplay_frontend.data.network.RetrofitInstance
import com.example.echoplay_frontend.data.models.User
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf

class UserSelectionViewModel : ViewModel() {
    var users = mutableStateOf<List<User>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                users.value = RetrofitInstance.api.getUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading.value = false
        }
    }
}