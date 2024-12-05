package com.example.agricurify.ui.imageDetector

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
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.agricurify.R
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.databinding.ActivityImageDetectorBinding
import com.example.agricurify.ui.result.ResultActivity
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import com.example.agricurify.utils.reduceFileImage
import com.example.agricurify.utils.uriToFile
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

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

        viewModel.isLoading.observe(this) {
            showLoading(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IMAGE_KEY, currentImageUri)
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentImageUri = savedInstanceState.getParcelable(IMAGE_KEY)
        showImage()
    }

    private fun uploadImage() {
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
        val file = File(cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        currentImageUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

        launcherIntentCamera.launch(currentImageUri!!)
    }


    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            currentImageUri?.let { uri ->
                startCrop(uri)
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
            startCrop(uri)
        } else {
            showToast(getString(R.string.empty_image_warning))
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "IMG_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(true)
            setStatusBarColor(resources.getColor(R.color.primary, null))
            setToolbarColor(resources.getColor(R.color.primary, null))
            setToolbarTitle(getString(R.string.crop_image))
            setToolbarWidgetColor(resources.getColor(R.color.white, null))
        }

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .start(this)
    }

    @Deprecated("This method has been deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                currentImageUri = resultUri
                showImage()
                binding.tvPreview.text = getFileNameFromUri(resultUri)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            showToast(getString(R.string.crop_error_message, cropError?.message))
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        if (fileName.isEmpty()) {
            fileName = uri.lastPathSegment ?: "Unknown"
        }
        return fileName
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image Uri", "ShowImage: $it")
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
