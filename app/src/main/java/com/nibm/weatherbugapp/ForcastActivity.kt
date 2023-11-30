package com.nibm.weatherbugapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import android.view.View
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ForcastActivity : AppCompatActivity() {


    private lateinit var txtLocation: TextView
    private lateinit var txtDate1: TextView
    private lateinit var textDescription1: TextView
    private lateinit var imageView1: ImageView
    private lateinit var txtDate2: TextView
    private lateinit var textDescription2: TextView
    private lateinit var imageView2: ImageView
    private lateinit var txtDate3: TextView
    private lateinit var textDescription3: TextView
    private lateinit var imageView3: ImageView
    private lateinit var txtDate4: TextView
    private lateinit var textDescription4: TextView
    private lateinit var imageView4: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBar: EditText
    private lateinit var txtTemp1:TextView
    private lateinit var txtTemp2:TextView
    private lateinit var txtTemp3:TextView
    private lateinit var txtTemp4:TextView
    private val apiKey = "7df53ab1cb496bd0eed2ef64eddec83e"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
    }

    private var isCurrentLocationForecast: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forcast)
        txtLocation = findViewById(R.id.txt_location)
        txtDate1 = findViewById(R.id.txtDate1)
        textDescription1 = findViewById(R.id.textDescription1)
        imageView1 = findViewById(R.id.imageView1)
        txtDate2 = findViewById(R.id.textDate2)
        textDescription2 = findViewById(R.id.textDescription2)
        imageView2 = findViewById(R.id.imageView2)
        txtDate3 = findViewById(R.id.textDate3)
        textDescription3 = findViewById(R.id.textDescription3)
        imageView3 = findViewById(R.id.imageView3)
        txtDate4 = findViewById(R.id.textDate4)
        textDescription4 = findViewById(R.id.textDescription4)
        imageView4 = findViewById(R.id.imageView4)
        txtTemp1 = findViewById(R.id.txtTemp1)
        txtTemp2 = findViewById(R.id.txtTemp2)
        txtTemp3 = findViewById(R.id.txtTemp3)
        txtTemp4 = findViewById(R.id.txtTemp4)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        searchBar = findViewById(R.id.search_bar)

        searchBar.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            // Define a margin for the clickable area
            val extraClickArea = 50

            if (event.action == MotionEvent.ACTION_UP) {
                val drawableBounds = searchBar.compoundDrawables[DRAWABLE_RIGHT].bounds

                // Adjust the condition to check within a specific area
                if (event.rawX >= (searchBar.right - drawableBounds.width() - extraClickArea)) {
                    onSearchButtonClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation();
            return
        }
        getCurrentLocation()
    }
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val forecastCityName = getCityName(location.latitude, location.longitude)
                        txtLocation.text = forecastCityName
                        getForecastWeatherData(location.latitude, location.longitude)
                    } else {
                        txtLocation.text = "Location Unknown"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error getting location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getForecastWeatherData(latitude: Double, longitude: Double) {
        val apiUrl =
            "https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&appid=$apiKey"

        val request = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { data ->
                try {
                    if (isCurrentLocationForecast) {
                        parseForecastData(data)
                    } else {
                        parseCityForecastData(data)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Error parsing forecast information",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(
                    this,
                    "Error loading forecast information",
                    Toast.LENGTH_SHORT
                ).show()
                error.printStackTrace()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun onSearchButtonClick() {
        val cityName = searchBar.text.toString()
        if (cityName.isNotEmpty()) {
            isCurrentLocationForecast = false
            getForecastForCity(cityName)
        } else {
            isCurrentLocationForecast = true
            getCurrentLocation()
        }
    }

    private fun getForecastForCity(cityName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(cityName, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val location = addresses[0]
                val latitude = location.latitude
                val longitude = location.longitude
                txtLocation.text = location.locality ?: location.adminArea ?: "Unknown"
                getForecastWeatherData(latitude, longitude)
            } else {
                Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun parseForecastData(data: JSONObject) {
        val forecastList = data.getJSONArray("list")
        val currentDate = Calendar.getInstance()

        for (i in 1 until 5) {
            val forecastItem = forecastList.getJSONObject(i * 8)
            val main = forecastItem.getJSONObject("main")
            val temperature = main.getDouble("temp")
            val temperatureInCelsius = temperature - 273.15
            val formattedTemperature = String.format("%.2f", temperatureInCelsius)
            val weatherArray = forecastItem.getJSONArray("weather")
            val weather = weatherArray.getJSONObject(0).getString("description").toUpperCase(Locale.getDefault())
            val icon = weatherArray.getJSONObject(0).getString("icon")
            val forecastDate = currentDate.clone() as Calendar
            forecastDate.add(Calendar.DAY_OF_MONTH, i) // Adjusted to get the forecast for the respective day

            val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(forecastDate.time)

            when (i) {
                1 -> {
                    txtDate1.text = "$date"
                    txtTemp1.text="${formattedTemperature}°C"
                    textDescription1.text = weather
                    loadWeatherIcon(icon, imageView1)
                }
                2 -> {
                    txtDate2.text = "$date"
                    txtTemp2.text="${formattedTemperature}°C"
                    textDescription2.text = weather
                    loadWeatherIcon(icon, imageView2)
                }
                3 -> {
                    txtDate3.text = "$date"
                    txtTemp3.text="${formattedTemperature}°C"
                    textDescription3.text = weather
                    loadWeatherIcon(icon, imageView3)
                }
                4 -> {
                    txtDate4.text = "$date"
                    txtTemp4.text="${formattedTemperature}°C"
                    textDescription4.text = weather
                    loadWeatherIcon(icon, imageView4)
                }
            }
        }
    }

    private fun parseCityForecastData(data: JSONObject) {
        try {
            val forecastList = data.getJSONArray("list")

            // take current date
            val currentDate = Calendar.getInstance()

            // getting next 4 days forcast
            for (i in 0 until 4) {
                val forecastItem = forecastList.getJSONObject(i * 8)
                val main = forecastItem.getJSONObject("main")
                val temperature = main.getDouble("temp")
                val weatherArray = forecastItem.getJSONArray("weather")
                val weather = weatherArray.getJSONObject(0).getString("description").toUpperCase(Locale.getDefault())
                val icon = weatherArray.getJSONObject(0).getString("icon")
                val temperatureInCelsius = temperature - 273.15
                val formattedTemperature = String.format("%.2f", temperatureInCelsius)

                // Corrected this part to advance the date correctly
                val forecastDate = currentDate.clone() as Calendar
                forecastDate.add(Calendar.DAY_OF_MONTH, i + 1) // Adjusted to get the forecast for the respective day

                val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(forecastDate.time)

                when (i) {
                    0 -> {
                        txtDate1.text = "$date"
                        txtTemp1.text="${formattedTemperature}°C"
                        textDescription1.text = weather
                        loadWeatherIcon(icon, imageView1)
                    }
                    1 -> {
                        txtDate2.text = "$date"
                        txtTemp2.text="${formattedTemperature}°C"
                        textDescription2.text = weather
                        loadWeatherIcon(icon, imageView2)
                    }
                    2 -> {
                        txtDate3.text = "$date"
                        txtTemp3.text="${formattedTemperature}°C"
                        textDescription3.text = weather
                        loadWeatherIcon(icon, imageView3)
                    }
                    3 -> {
                        txtDate4.text = "$date"
                        txtTemp4.text="${formattedTemperature}°C"
                        textDescription4.text = weather
                        loadWeatherIcon(icon, imageView4)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error parsing forecast information",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun loadWeatherIcon(icon: String, imageView: ImageView) {
        val iconUrl = "https://openweathermap.org/img/w/$icon.png"
        Picasso.get().load(iconUrl).into(imageView)
    }

    private fun getCityName(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val cityName = addresses[0]?.locality
                    return cityName ?: "Unknown"
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Unknown"
    }




}