package com.example.agricurify.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agricurify.data.di.Injection
import com.example.agricurify.data.preference.Preference
import com.example.agricurify.data.repository.MainRepository

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val mRepository: MainRepository,
    private val mPreference: Preference
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(mRepository, mPreference) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object{
        @Volatile
        private var instance: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory =
            instance ?: synchronized(this){
                instance ?: ViewModelFactory(
                    Injection.provideMainRepository(context),
                    Injection.userPreference(context)
                )
            }.also { instance = it }
    }
}