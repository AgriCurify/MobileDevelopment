package com.example.agricurify.ui.home

import android.Manifest
import android.annotation.SuppressLint
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
import com.bumptech.glide.Glide
import com.example.agricurify.data.response.WeatherResponse
import com.example.agricurify.databinding.FragmentHomeBinding
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
        val root: View = binding.root


        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi", "SetTextI18n", "DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity?)?.supportActionBar?.apply {
            setShowHideAnimationEnabled(false)
            hide()
        }

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        viewModel.getWeatherData()
        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                updateCurrentWeather(weather)
                updateForecastWeather(weather)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateCurrentWeather(weather: WeatherResponse) {
        val currentDateTime = ZonedDateTime.now(ZoneId.systemDefault()) // Gunakan ZonedDateTime
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val matchWeather = weather.list.firstOrNull { forecast ->
            val forecastDateTime = LocalDateTime.parse(forecast.dtTxt, formatter)
                .atZone(ZoneId.of("UTC")) // Tentukan zona waktu UTC
                .withZoneSameInstant(ZoneId.systemDefault()) // Konversi ke zona waktu perangkat
            forecastDateTime.isAfter(currentDateTime) || forecastDateTime.isEqual(currentDateTime)
        }

        matchWeather?.let { forecast ->
            val tempInKelvin = forecast.main.temp
            val tempInCelcius = tempInKelvin - 273
            binding.tvTemp.text = String.format("%d°C", tempInCelcius.toInt())

            binding.tvHumidity.text = "Kelembapan : ${forecast.main.humidity}%"
            val iconCode = forecast.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
            Glide.with(requireActivity())
                .load(iconUrl)
                .into(binding.imgCuaca)

            val formattedDate = formatDate(forecast.dtTxt)
            binding.tvdate.text = formattedDate
        }
    }

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
            val tempInKelvin = weatherItem.main.temp
            val tempInCelcius = tempInKelvin - 273

            val (tempView, humidityView, imageView) = cardViews[index]
            tempView.text = String.format("%d°C", tempInCelcius.toInt())
            humidityView.text = "Kelembapan : ${weatherItem.main.humidity}%"

            val iconCode = weatherItem.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
            Glide.with(requireActivity())
                .load(iconUrl)
                .into(imageView)

            val formatDate = formatDateInDay(weatherItem.dtTxt)
            dateViews[index].text = formatDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}