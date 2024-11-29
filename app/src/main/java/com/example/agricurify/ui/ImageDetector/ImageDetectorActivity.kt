package com.example.agricurify.ui.ImageDetector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.agricurify.MainActivity
import com.example.agricurify.R
import com.example.agricurify.databinding.ActivityImageDetectorBinding
import com.example.agricurify.ui.result.ResultActivity
import com.example.agricurify.utils.getImageUri

class ImageDetectorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetectorBinding
    private lateinit var viewModel: ImageDetectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnBack.setOnClickListener {
            finish()
        }

        viewModel = ViewModelProvider(this)[ImageDetectorViewModel::class.java]

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.clipButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.btnScan.setOnClickListener {
            viewModel.currentImageUri.value?.let { uri ->
                viewModel.classifyImage(uri)
            } ?: showToast(getString(R.string.empty_image_warning))
        }
    }

    private fun observeViewModel() {
        viewModel.currentImageUri.observe(this) { uri ->
            uri?.let {
                binding.previewImageView.setImageURI(it)
                binding.tvPreview.text = getFileNameFromUri(it)
            }
        }

        viewModel.classificationResult.observe(this) { result ->
            result?.let { (label, score) ->
                moveToResult(label, score)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }
    }

    private fun moveToResult(label: String, score: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_LABEL, label)
            putExtra(ResultActivity.EXTRA_SCORE, score)
            putExtra(ResultActivity.EXTRA_IMAGE_URI, viewModel.currentImageUri.value.toString())
        }
        startActivity(intent)
    }

    private fun startCamera() {
        val (uri, _) = getImageUri(this)
        viewModel.setCurrentImageUri(uri)
        launcherIntentCamera.launch(uri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            viewModel.currentImageUri.value?.let {
                showImage()
            }
        } else {
            viewModel.setCurrentImageUri(null)
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
        uri?.let {
            viewModel.setCurrentImageUri(it)
            showImage()
        } ?: showToast(getString(R.string.empty_image_warning))
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
        viewModel.currentImageUri.value?.let {
            binding.previewImageView.setImageURI(it)
        }
    }

}
