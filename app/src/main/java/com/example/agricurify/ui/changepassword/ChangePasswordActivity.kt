package com.example.agricurify.ui.changepassword

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agricurify.data.preference.dataStore
import com.example.agricurify.data.remote.ApiConfig
import com.example.agricurify.data.response.ChangePasswordRequest
import com.example.agricurify.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSubmit.setOnClickListener {
            val oldPassword = binding.edOldpass.text.toString().trim()
            val newPassword = binding.edNewpass.text.toString().trim()
            val confirmPassword = binding.edConfirmnewpass.text.toString().trim()

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Harap isi semua bidang.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Password baru dan konfirmasi tidak cocok.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(oldPassword, newPassword, confirmPassword)
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        val apiService = ApiConfig.authentication()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getToken() // Ambil token dari DataStore
                val request = ChangePasswordRequest(oldPassword, newPassword, confirmPassword)
                val response = apiService.changePassword("Bearer $token", request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "Password berhasil diubah"
                        Toast.makeText(this@ChangePasswordActivity, message, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Gagal mengubah password: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun getToken(): String {
        val preference = com.example.agricurify.data.preference.Preference.getInstance(applicationContext.dataStore)
        return preference.getToken().first()
    }
}
