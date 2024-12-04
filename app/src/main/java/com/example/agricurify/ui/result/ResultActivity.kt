package com.example.agricurify.ui.result

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agricurify.R
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.databinding.ActivityResultBinding

@Suppress("DEPRECATION")
class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnBack.setOnClickListener {
            finish()
        }


        val imageString = intent.getStringExtra(EXTRA_IMAGE_URI)
        imageString?.let {
            val imageUri = Uri.parse(it)
            binding.previewImageView.setImageURI(imageUri)
        }
        val modelResponse = intent.getParcelableExtra<ModelResponse>(EXTRA_MODEL_RESPONSE)
        if (modelResponse != null) {
            displayResult(modelResponse)
        }

    }

    @SuppressLint("SetTextI18n")
    private fun displayResult(response: ModelResponse) {
        binding.tvName.text = response.diseaseInfo.name
        binding.tvScore.text = "Akurasi : ${response.confidence}%"
        binding.tvDescription.text = response.diseaseInfo.description

        val treatmentList = response.diseaseInfo.treatment
        val treatmentText = StringBuilder("")
        for ((index, treatment) in treatmentList.withIndex()) {
            treatmentText.append("${index + 1}. $treatment\n")
        }
        binding.tvTreatment.text = treatmentText.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ResultActivity", "Destroyed")
    }


    companion object {
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_MODEL_RESPONSE = "extra_model_response"

    }
}