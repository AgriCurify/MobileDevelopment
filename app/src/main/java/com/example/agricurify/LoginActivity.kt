package com.example.agricurify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agricurify.databinding.ActivityLoginBinding
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import com.example.agricurify.utils.ResultState
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.apply {
            tvSignUp.setOnClickListener {
                navigateToRegister()
            }
            btnLogin.setOnClickListener {
                lifecycleScope.launch {
                    login()
                }
            }
        }
    }

    private suspend fun login() {
        val email = binding.edRegisterEmail.text.toString().trim()
        val password = binding.edRegisterPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.edRegisterEmail.error = getString(R.string.empty_warning)
            }
            password.isEmpty() -> {
                binding.edRegisterPassword.error = getString(R.string.empty_warning)
            }
            else -> {
                viewModel.login(email, password).observe(this){result ->
                    if (result != null) {
                        when(result){
                            is ResultState.Loading -> {
                                showLoading()
                            }
                            is ResultState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                navigateToMain()
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show()
    }
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }
}
