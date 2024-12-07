package com.example.agricurify.ui.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.agricurify.data.database.AppDatabase
import com.example.agricurify.data.database.HistoryEntity
import com.example.agricurify.databinding.ActivityDetailHistoryBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailHistoryBinding
    private lateinit var database: AppDatabase

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = AppDatabase.getDatabase(this)

        @Suppress("DEPRECATION") val history = intent.getParcelableExtra<HistoryEntity>(EXTRA_HISTORY)
        history?.let {
            binding.tvName.text = it.diseaseName
            binding.tvDescription.text = it.description
            binding.tvScore.text = "Akurasi: ${it.confidence}%"
            displayTreatments(it.treatments)
            Glide.with(this).load(it.imageUri).into(binding.previewImageView)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnTranslate.setOnClickListener {
            history?.let {
                binding.progressBar.visibility = View.VISIBLE
                translateContent(it)
            }
        }

        binding.btnDelete.setOnClickListener {
            history?.let {
                deleteHistory(it)
            }
        }
    }

    private fun displayTreatments(treatments: String) {
        val treatmentList = treatments.split("\n") // Misalkan data dipisahkan oleh newline.
        val formattedText = StringBuilder()
        treatmentList.forEachIndexed { index, treatment ->
            formattedText.append("${index + 1}. $treatment\n")
        }
        binding.tvTreatment.text = formattedText.toString()
    }

    private fun translateContent(history: HistoryEntity) {
        translateText(history.diseaseName) { translatedName ->
            binding.tvName.text = translatedName
        }

        translateText(history.description) { translatedDescription ->
            binding.tvDescription.text = translatedDescription
        }

        val treatments = history.treatments.split("\n")
        val translatedTreatments = StringBuilder()

        treatments.forEachIndexed { index, treatment ->
            translateText(treatment) { translatedTreatment ->
                translatedTreatments.append("${index + 1}. $translatedTreatment\n")
                binding.tvTreatment.text = translatedTreatments.toString()
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
            .requireWifi()
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
                        showToast("Translation failed: ${exception.message}")
                        translator.close()
                        binding.progressBar.visibility = View.GONE
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Model download failed: ${exception.message}")
                translator.close()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun deleteHistory(history: HistoryEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.historyDao().deleteHistory(history)
                launch(Dispatchers.Main) {
                    showToast("Riwayat berhasil dihapus.")
                    finish()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    showToast("Gagal menghapus riwayat: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_HISTORY = "extra_history"
    }
}
