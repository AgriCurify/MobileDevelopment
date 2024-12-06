package com.example.agricurify.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.agricurify.adapter.HistoryAdapter
import com.example.agricurify.data.database.AppDatabase
import com.example.agricurify.data.response.WeatherResponse
import com.example.agricurify.databinding.FragmentHomeBinding
import com.example.agricurify.ui.allhistory.AllHistoryActivity
import com.example.agricurify.ui.detail.DetailHistoryActivity
import com.example.agricurify.ui.viewmodel.MainViewModel
import com.example.agricurify.ui.viewmodel.ViewModelFactory
import com.example.agricurify.utils.formatDate
import com.example.agricurify.utils.formatDateInDay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: HistoryAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            viewModel.getWeatherData()
        } else {
            Toast.makeText(requireActivity(), "Akses lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = ViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi", "SetTextI18n", "DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar()
        setupHistorySection()
        setupWeatherSection()

        binding.tvHistoryNav.setOnClickListener {
            val intent = Intent(requireContext(), AllHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupActionBar() {
        (activity as AppCompatActivity?)?.supportActionBar?.apply {
            hide()
        }
    }

    private fun setupHistorySection() {
        val database = AppDatabase.getDatabase(requireContext())
        val historyDao = database.historyDao()

        adapter = HistoryAdapter { history ->
            val intent = Intent(requireContext(), DetailHistoryActivity::class.java)
            intent.putExtra(DetailHistoryActivity.EXTRA_HISTORY, history)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter

        // Observasi data dari database
        historyDao.getAllHistories().observe(viewLifecycleOwner) { histories ->
            adapter.submitList(histories)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupWeatherSection() {
        if (viewModel.weatherData.value == null) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            viewModel.getWeatherData()
        }

        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                updateCurrentWeather(weather)
                updateForecastWeather(weather)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            showLoading(it)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateCurrentWeather(weather: WeatherResponse) {
        val currentDateTime = ZonedDateTime.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val matchWeather = weather.list.firstOrNull { forecast ->
            val forecastDateTime = LocalDateTime.parse(forecast.dtTxt, formatter)
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
            forecastDateTime.isAfter(currentDateTime) || forecastDateTime.isEqual(currentDateTime)
        }

        matchWeather?.let { forecast ->
            val tempInCelcius = forecast.main.temp - 273
            binding.tvTemp.text = String.format("%d°C", tempInCelcius.toInt())
            binding.tvHumidity.text = "Kelembapan : ${forecast.main.humidity}%"
            val iconUrl = "https://openweathermap.org/img/w/${forecast.weather[0].icon}.png"
            Glide.with(requireActivity()).load(iconUrl).into(binding.imgCuaca)
            binding.tvdate.text = formatDate(forecast.dtTxt)
            binding.tvLocation.text = "${weather.city.name}, ${Locale("", weather.city.country).displayCountry}"
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateForecastWeather(weather: WeatherResponse) {
        val cardViews = listOf(
            Triple(binding.tvCardTemp1, binding.tvCardHumidity1, binding.ivCuaca1),
            Triple(binding.tvCardTemp2, binding.tvCardHumidity2, binding.ivCuaca2),
            Triple(binding.tvCardTemp3, binding.tvCardHumidity3, binding.ivCuaca3),
            Triple(binding.tvCardTemp4, binding.tvCardHumidity4, binding.ivCuaca4)
        )

        val dateViews = listOf(
            binding.tvDay1,
            binding.tvDay2,
            binding.tvDay3,
            binding.tvDay4
        )

        val indices = listOf(0, 8, 16, 24)

        indices.forEachIndexed { index, idx ->
            val weatherItem = weather.list[idx]
            val tempInCelcius = weatherItem.main.temp - 273
            val (tempView, humidityView, imageView) = cardViews[index]
            tempView.text = String.format("%d°C", tempInCelcius.toInt())
            humidityView.text = "Kelembapan : ${weatherItem.main.humidity}%"
            val iconUrl = "https://openweathermap.org/img/w/${weatherItem.weather[0].icon}.png"
            Glide.with(requireActivity()).load(iconUrl).into(imageView)
            dateViews[index].text = formatDateInDay(weatherItem.dtTxt)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardView.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.cardForecast.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
