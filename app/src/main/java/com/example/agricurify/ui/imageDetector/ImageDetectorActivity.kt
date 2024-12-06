package com.example.agricurify.ui.imageDetector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private var selectedFruit: String? = null
    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory.getInstance(this)
    }

    private var currentImageUri: Uri? = null
    private var croppedFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val items = listOf("Apel", "Anggur")
        val adapter = ArrayAdapter(this, R.layout.item_list, items)

        binding.autoComplete.setAdapter(adapter)
        binding.autoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            selectedFruit = parent.getItemAtPosition(position).toString()
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.clipButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.btnScan.setOnClickListener {
            lifecycleScope.launch {
                when (selectedFruit) {
                    "Apel" -> uploadAppleImage()
                    "Anggur" -> uploadGrapeImage()
                    else -> showToast(getString(R.string.select_item_warning))
                }
            }
        }

        viewModel.isLoading.observe(this) {
            showLoading(it)
        }

        viewModel.croppedFileName.observe(this) { fileName ->
            binding.tvPreview.text = fileName ?: getString(R.string.default_file_name)
        }
        viewModel.croppedImageUri.observe(this) { uri ->
            binding.previewImageView.setImageURI(uri)
        }

    }

    private fun uploadAppleImage() {
        currentImageUri?.let { uri ->
            try {
                val imageFile = uriToFile(uri, this).reduceFileImage()
                Log.d("Image File Path", "File Path: ${imageFile.path}, Exists: ${imageFile.exists()}")
                showLoading(true)

                viewModel.uploadAppleImage(imageFile)
                viewModel.modelData.observe(this) { response ->
                    response?.let { moveToResultActivity(it) }
                    showLoading(false)
                }
            } catch (e: Exception) {
                Log.e("Upload Error", "Error preparing file for upload: ${e.message}")
                viewModel.errorMessage.observe(this) { errorMessage ->
                    showToast(errorMessage ?: getString(R.string.upload_error_message))

                }
            } finally {
                showLoading(false)
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun uploadGrapeImage() {
        currentImageUri?.let { uri ->
            try {
                val imageFile = uriToFile(uri, this).reduceFileImage()
                Log.d("Image File Path", "File Path: ${imageFile.path}, Exists: ${imageFile.exists()}")
                showLoading(true)

                viewModel.uploadGrapeImage(imageFile)
                viewModel.modelData.observe(this) { response ->
                    response?.let { moveToResultActivity(it) }
                    showLoading(false)
                }
            } catch (e: Exception) {
                Log.e("Upload Error", "Error preparing file for upload: ${e.message}")
                viewModel.errorMessage.observe(this) { errorMessage ->
                    showToast(errorMessage ?: getString(R.string.upload_error_message))

                }
            } finally {
                showLoading(false)
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun moveToResultActivity(response: ModelResponse) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
            putExtra(ResultActivity.EXTRA_MODEL_RESPONSE, response)
            // Tambahkan flag untuk mengatur stack activity
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
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
            currentImageUri?.let { uri -> startCrop(uri) }
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
        val destinationFile = File(cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        val destinationUri = Uri.fromFile(destinationFile)

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

        croppedFileName = destinationFile.name
    }

    @Deprecated("This method has been deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                currentImageUri = resultUri
                viewModel.setCroppedFileName(croppedFileName ?: "Unknown")
                viewModel.setCroppedImageUri(resultUri)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            showToast(getString(R.string.crop_error_message, cropError?.message))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
