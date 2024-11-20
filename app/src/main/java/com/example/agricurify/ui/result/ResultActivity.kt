package com.example.agricurify.ui.result

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agricurify.R
import com.example.agricurify.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val label = intent.getStringExtra(EXTRA_LABEL)
        val score = intent.getStringExtra(EXTRA_SCORE)
        val imageString = intent.getStringExtra(EXTRA_IMAGE_URI)
        imageString?.let {
            val imageUri = Uri.parse(it)
            binding.previewImageView.setImageURI(imageUri)
        }

        binding.tvName.text = label
        binding.tvScore.text = "Akurasi : $score"

    }


    companion object {
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_IMAGE_URI = "extra_image_uri"

    }
}