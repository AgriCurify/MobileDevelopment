package com.example.agricurify.ui.ImageDetector

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agricurify.helper.ImageClassifierHelper
import org.tensorflow.lite.task.vision.classifier.Classifications

class ImageDetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentImageUri = MutableLiveData<Uri?>()
    val currentImageUri: LiveData<Uri?> get() = _currentImageUri

    private val _classificationResult = MutableLiveData<Pair<String, String>?>()
    val classificationResult: LiveData<Pair<String, String>?> get() = _classificationResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val imageClassifierHelper: ImageClassifierHelper

    init {
        _isLoading.value = false

        imageClassifierHelper = ImageClassifierHelper(
            context = application,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    _isLoading.postValue(false)
                    _error.postValue(error)
                }

                override fun onResult(results: List<Classifications>?) {
                    _isLoading.postValue(false)
                    results?.firstOrNull()?.categories?.firstOrNull()?.let {
                        val label = it.label
                        val score = "%.2f%%".format(it.score * 100)
                        _classificationResult.postValue(Pair(label, score))
                    }
                }
            }
        )
    }

    fun setCurrentImageUri(uri: Uri?) {
        _currentImageUri.value = uri
    }

    fun classifyImage(uri: Uri) {
        _isLoading.value = true
        imageClassifierHelper.classifyStaticImage(uri)
    }

    fun clearError() {
        _error.value = null
    }
}
