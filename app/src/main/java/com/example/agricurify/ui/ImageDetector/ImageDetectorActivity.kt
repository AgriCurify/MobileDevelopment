package com.example.agricurify.ui.ImageDetector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agricurify.R
import com.example.agricurify.databinding.ActivityImageDetectorBinding
import com.example.agricurify.helper.ImageClassifierHelper
import com.example.agricurify.ui.result.ResultActivity
import com.example.agricurify.utils.getImageUri
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.text.NumberFormat

class ImageDetectorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetectorBinding

    private var currentImageUri: Uri? = null

    private lateinit var imageClassifierHelper: ImageClassifierHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.clipButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener{ startCamera() }
        binding.btnScan.setOnClickListener {
            analyzeImage()
        }

        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        binding.progressIndicator.visibility = View.GONE
                        showToast(error)
                    }
                }

                override fun onResult(results: List<Classifications>?) {
                    runOnUiThread {
                        binding.progressIndicator.visibility = View.GONE
                        results?.let {
                            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                                val label = it[0].categories[0].label
                                val score = NumberFormat.getPercentInstance().format(it[0].categories[0].score).trim()

                                moveToResult(label, score)

                            }
                        }
                    }
                }

            }
        )


    }

    private fun analyzeImage() {

        currentImageUri?.let { uri ->
            imageClassifierHelper.classifyStaticImage(uri)
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun moveToResult(label: String, score: String) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_LABEL, label)
        intent.putExtra(ResultActivity.EXTRA_SCORE, score)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
        startActivity(intent)
    }

    private fun startCamera() {
        val (uri, imaName) = getImageUri(this)
        currentImageUri = uri
        launcherIntentCamera.launch(currentImageUri!!)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ){ isSuccess ->
        if (isSuccess) {
            showImage()
            val (uri, imageName) = getImageUri(this)
            binding.tvPreview.text = imageName
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
    ) {uri : Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()

            val imageName = getFileNameFromUri(uri)
            binding.tvPreview.text = imageName
        } else {
            showToast(getString(R.string.empty_image_warning))
        }
    }

    private fun getFileNameFromUri(uri: Uri) : String {
        var imageName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != 1){
                    imageName = it.getString(displayNameIndex)
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
            binding.previewImageView.setImageURI(it)
        }
    }
}