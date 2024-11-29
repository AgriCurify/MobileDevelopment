package com.example.agricurify.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.agricurify.data.repository.MainRepository
import com.example.agricurify.data.response.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainViewModel(private val repository: MainRepository) : ViewModel() {
    private val _weatherData = MutableLiveData<WeatherResponse?>(null)
    val weatherData: LiveData<WeatherResponse?> = _weatherData

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun getWeatherData() {
        viewModelScope.launch {
            try {
                val response = repository.getWeatherData()
                _weatherData.value = response
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }
}