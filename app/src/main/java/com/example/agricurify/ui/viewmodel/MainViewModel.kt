package com.example.agricurify.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agricurify.data.repository.MainRepository
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.data.response.WeatherResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File

class MainViewModel(private val repository: MainRepository) : ViewModel() {
    private val _weatherData = MutableLiveData<WeatherResponse?>(null)
    val weatherData: LiveData<WeatherResponse?> = _weatherData

    private val _modelData = MutableLiveData<ModelResponse?>(null)
    val modelData: LiveData<ModelResponse?> = _modelData


    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun getWeatherData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getWeatherData()
                _weatherData.value = response
                _isLoading.value = false
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun uploadAppleImage(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getAppleDetection(file)
                _modelData.value = response
                Log.d("MainViewModel", "uploadImage: $response")
                _isLoading.value = false
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}