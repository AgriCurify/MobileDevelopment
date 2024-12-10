package com.example.agricurify.ui.result

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agricurify.data.database.AppDatabase
import com.example.agricurify.data.database.HistoryEntity
import com.example.agricurify.data.response.ModelResponse
import com.example.agricurify.databinding.ActivityResultBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var database: AppDatabase

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = AppDatabase.getDatabase(this)

        val imageString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val modelResponse = intent.getParcelableExtra<ModelResponse>(EXTRA_MODEL_RESPONSE)

        if (imageString != null && modelResponse != null) {
            val imageUri = Uri.parse(imageString)
            binding.previewImageView.setImageURI(imageUri)
            displayResult(modelResponse)
            saveHistory(modelResponse, imageString)
        }

        binding.btnBack.setOnClickListener {
            finish()
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
        val treatmentText = StringBuilder()
        treatmentList.forEachIndexed { index, treatment ->
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
        val treatmentText = StringBuilder()
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
        val translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(inputText)
                    .addOnSuccessListener { translatedText ->
                        callback(translatedText)
                        translator.close()
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { exception ->
                        showToast("Terjemahan gagal: ${exception.message}")
                        translator.close()
                        binding.progressBar.visibility = View.GONE
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Unduhan model gagal: ${exception.message}")
                translator.close()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun saveHistory(response: ModelResponse, imageUri: String) {
        val history = HistoryEntity(
            diseaseName = response.diseaseInfo.name,
            description = response.diseaseInfo.description,
            confidence = response.confidence,
            treatments = response.diseaseInfo.treatment.joinToString("\n"),
            imageUri = imageUri
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.historyDao().insertHistory(history)
                launch(Dispatchers.Main) {
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    showToast("Gagal menyimpan riwayat: ${e.message}")
                }
            }
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
