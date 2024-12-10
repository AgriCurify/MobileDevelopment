package com.example.agricurify.ui.allhistory

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agricurify.adapter.HistoryAdapter
import com.example.agricurify.data.database.AppDatabase
import com.example.agricurify.databinding.ActivityAllHistoryBinding
import com.example.agricurify.ui.detail.DetailHistoryActivity
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllHistoryBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val factory = ViewModelFactory.getInstance(application)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        adapter = HistoryAdapter { history ->
            val intent = Intent(this@AllHistoryActivity, DetailHistoryActivity::class.java)
            intent.putExtra(DetailHistoryActivity.EXTRA_HISTORY, history)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter

        val database = AppDatabase.getDatabase(this)
        val historyDao = database.historyDao()

        historyDao.getAllHistories().observe(this) { histories ->
            if (histories.isEmpty()) {
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmptyHistory.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(histories)
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    historyDao.deleteAllHistories()
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@AllHistoryActivity, "Semua riwayat dihapus", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@AllHistoryActivity, "Error menghapus semua riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
