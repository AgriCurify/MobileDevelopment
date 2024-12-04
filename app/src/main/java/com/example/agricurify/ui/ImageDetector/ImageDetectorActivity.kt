package com.example.agricurify.ui.ImageDetector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.agricurify.R
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.databinding.ActivityImageDetectorBinding
import com.example.agricurify.ui.result.ResultActivity
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import com.example.agricurify.utils.getImageUri
import com.example.agricurify.utils.reduceFileImage
import com.example.agricurify.utils.uriToFile
import kotlinx.coroutines.launch

class ImageDetectorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetectorBinding
    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory.getInstance(this)
    }

    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.clipButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.btnScan.setOnClickListener {
            lifecycleScope.launch {
                uploadImage()
            }
        }

        viewModel.isLoading.observe(this){
            showLoading(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IMAGE_KEY, currentImageUri)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        @Suppress("DEPRECATION")
        currentImageUri = savedInstanceState.getParcelable(IMAGE_KEY)
        showImage()
    }

    private suspend fun uploadImage() {
        currentImageUri?.let { uri ->
            try {
                val imageFile = uriToFile(uri, this).reduceFileImage()
                Log.d("Image File Path", "File Path: ${imageFile.path}, Exists: ${imageFile.exists()}")
                showLoading(true)

                viewModel.uploadAppleImage(imageFile)
                viewModel.modelData.observe(this) { response ->
                    response?.let {
                        Log.d("ImageDetector", "Response: $response")
                        moveToResultActivity(it)

                    }
                    showLoading(false)
                }

            } catch (e: Exception) {
                Log.e("Upload Error", "Error preparing file for upload: ${e.message}")
            } finally {
                showLoading(false)
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }



    private fun moveToResultActivity(response: ModelResponse) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
            putExtra(ResultActivity.EXTRA_MODEL_RESPONSE, response)

        }
        startActivity(intent)
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri!!)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
            currentImageUri?.let { uri ->
                binding.tvPreview.text = getFileNameFromUri(uri)
            }

        } else {
            currentImageUri = null
        }
    }

    private fun startGallery() {
        launchGallery.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private val launchGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
            binding.tvPreview.text = getFileNameFromUri(uri)
        } else {
            showToast(getString(R.string.empty_image_warning))

        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var imageName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    imageName = cursor.getString(nameIndex)
                }
            }
        }
        return imageName
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Imgae Uri", "ShowImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE

    }

    companion object {
        private const val IMAGE_KEY = "currentImageUri"
    }

}
