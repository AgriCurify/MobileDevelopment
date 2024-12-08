package com.example.agricurify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agricurify.databinding.ActivityRegisterBinding
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import com.example.agricurify.utils.ResultState
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.signupButton.setOnClickListener {
            lifecycleScope.launch {
                register()
            }
        }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }

    private suspend fun register() {
        val name = binding.edRegisterNama.text.toString().trim()
        val email = binding.edRegisterEmail.text.toString().trim()
        val password = binding.edRegisterPassword.text.toString().trim()

        when {
            name.isEmpty() -> {
                binding.edRegisterNama.error = getString(R.string.empty_warning)
            }
            email.isEmpty() -> {
                binding.edRegisterEmail.error = getString(R.string.empty_warning)
            }
            password.isEmpty() -> {
                binding.edRegisterPassword.error = getString(R.string.empty_warning)
            }
            else -> {
                viewModel.register(name, email, password).observe(this) { result ->
                    if (result != null) {
                        when (result) {
                            is ResultState.Loading -> {
                                showLoading()
                            }
                            is ResultState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                loginNavigation()
                                showToast(result.data.message)
                            }
                            is ResultState.Error -> {
                                showToast(result.error)
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loginNavigation() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
