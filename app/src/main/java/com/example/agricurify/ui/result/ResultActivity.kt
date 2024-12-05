package com.example.agricurify.ui.result

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.databinding.ActivityResultBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

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
        modelResponse?.let {
            displayResult(it)
        }

        binding.btnTranslate.setOnClickListener {
            modelResponse?.let { response ->
                binding.progressBar.visibility = View.VISIBLE
                translateContent(response)
            }
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

    private fun translateContent(response: ModelResponse) {
        translateText(response.diseaseInfo.name) { translatedName ->
            binding.tvName.text = translatedName
        }

        translateText(response.diseaseInfo.description) { translatedDescription ->
            binding.tvDescription.text = translatedDescription
        }

        val treatmentList = response.diseaseInfo.treatment
        val treatmentText = StringBuilder("")
        treatmentList.forEachIndexed { index, treatment ->
            translateText(treatment) { translatedTreatment ->
                treatmentText.append("${index + 1}. $translatedTreatment\n")
                binding.tvTreatment.text = treatmentText.toString()
            }
        }
    }

    private fun translateText(inputText: String, callback: (String) -> Unit) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.INDONESIAN)
            .build()
        val indonesianEnglishTranslator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        indonesianEnglishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                indonesianEnglishTranslator.translate(inputText)
                    .addOnSuccessListener { translatedText ->
                        callback(translatedText)
                        indonesianEnglishTranslator.close()
                        binding.progressBar.visibility = View.GONE

                    }
                    .addOnFailureListener { exception ->
                        showToast("Translation failed: ${exception.message}")
                        indonesianEnglishTranslator.close()
                        binding.progressBar.visibility = View.GONE
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Model download failed: ${exception.message}")
                indonesianEnglishTranslator.close()
                binding.progressBar.visibility = View.GONE
            }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("ResultActivity", "Destroyed")
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_MODEL_RESPONSE = "extra_model_response"
    }
}