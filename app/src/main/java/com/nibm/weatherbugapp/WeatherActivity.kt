package com.nibm.weatherbugapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WeatherActivity : AppCompatActivity() {

    private lateinit var txtDataAndTime: TextView
    private lateinit var txtCountry: TextView
    private lateinit var txtCelcius2: TextView
    private lateinit var txtDescription: TextView
    private lateinit var imgWeatherImg: ImageView
    private lateinit var txtPressureDetails: TextView
    private lateinit var txtHumidityDetails: TextView
    private lateinit var txtTempDetails: TextView
    private lateinit var txtWeatherDetails: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = "7df53ab1cb496bd0eed2ef64eddec83e"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_home)
        val button: Button = findViewById(R.id.button)


        button.setOnClickListener(View.OnClickListener {
            // Create an Intent to navigate to ForeCastDashBoard
            val intent = Intent(this@WeatherActivity, ForcastActivity::class.java)
            startActivity(intent)
        })
        txtDataAndTime = findViewById(R.id.txt_dataAndTime)
        txtCountry = findViewById(R.id.txt_country)
        txtCelcius2 = findViewById(R.id.txt_celcius2)
        txtDescription = findViewById(R.id.txt_description)
        imgWeatherImg = findViewById(R.id.img_weatherImg)
        txtPressureDetails = findViewById(R.id.txt_pressureDetails)
        txtHumidityDetails = findViewById(R.id.txt_humidityDetails)
        txtTempDetails = findViewById(R.id.txt_TempDetails)
        txtWeatherDetails = findViewById(R.id.txt_WindSpeedDetails)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        // Get the current date and time
        getCurrentDateTime()
        getCurrentLocation()
        requestPermission()
        // Get the current location, temperature, weather description, and weather icon for the current location
        CoroutineScope(Dispatchers.Main).launch {
            delay(4000)

            getCurrentLocation()
        }



        // Adding the new code for handle the drawableRight click
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchBar.right - searchBar.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {

                    onSearchButtonClick()
                    return@setOnTouchListener true
                }
            }
            false
        }


    }

    private fun onSearchButtonClick() {

        val cityName = findViewById<EditText>(R.id.search_bar).text.toString()
        if (cityName.isNotEmpty()) {
            // trigger below method to get weather information for the searched city
            getWeatherForCity(cityName)
        } else {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentDateTime() {
        // Get the current date and time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM dd (EEE) | hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(calendar.time)

        // Display the formatted date and time
        txtDataAndTime.text = formattedDate
    }

    private fun requestPermission()
    {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    private fun getCurrentLocation() {

        // Check location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Use fusedLocationClient to get the current location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {

                       // getWeather function trigger to get weather details
                        getWeatherData(location.latitude, location.longitude)
                    } else {

                        // Handle the case when location is null
                        txtCountry.text = "Location: Unknown"
                    }
                }
                .addOnFailureListener { e ->
                    // Handle errors
                    Toast.makeText(
                        this,
                        "Error getting location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getWeatherData(latitude: Double, longitude: Double) {
        val apiUrl =
            "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey"
        val request = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { data ->
                try {
                    val cityName = data.getString("name")
                    val temperature = data.getJSONObject("main").getDouble("temp")
                    val temperatureInCelsius = temperature - 273.15
                    val formattedTemperature = String.format("%.2f", temperatureInCelsius)
                    val pressure = data.getJSONObject("main").getDouble("pressure")
                    val humidity = data.getJSONObject("main").getDouble("humidity")
                    val windSpeed = data.getJSONObject("wind").getDouble("speed")
                    val weatherArray = data.getJSONArray("weather")
                    val description = if (weatherArray.length() > 0) {
                        weatherArray.getJSONObject(0).getString("description")
                    } else {
                        ""
                    }
                    val iconCode = if (weatherArray.length() > 0) {
                        weatherArray.getJSONObject(0).getString("icon")
                    } else {
                        ""
                    }

                    // Display details
                    txtCountry.text = cityName
                    txtCelcius2.text = "${formattedTemperature}°C"
                    txtDescription.text = description.toUpperCase()

                    // Display the additional weather details
                    txtPressureDetails.text = "$pressure hPa"
                    txtHumidityDetails.text = "$humidity%"
                    txtTempDetails.text = "$temperature°C"
                    txtWeatherDetails.text = "${windSpeed} m/s"

                    // Display the weather icon
                    displayWeatherIcon(iconCode)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Error parsing weather information",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(
                    this,
                    "Error loading weather information",
                    Toast.LENGTH_SHORT
                ).show()
                error.printStackTrace()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun getWeatherForCity(cityName: String) {
        val apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=$cityName&appid=$apiKey"
        val request = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { data ->
                // Handle the JSON response
                try {
                    // Extract weather information and update UI
                    val temperature = data.getJSONObject("main").getDouble("temp")
                    val temperatureInCelsius = temperature - 273.15
                    val formattedTemperature = String.format("%.2f", temperatureInCelsius)
                    val pressure = data.getJSONObject("main").getDouble("pressure")
                    val humidity = data.getJSONObject("main").getDouble("humidity")
                    val windSpeed = data.getJSONObject("wind").getDouble("speed")
                    val weatherArray = data.getJSONArray("weather")
                    val description = if (weatherArray.length() > 0) {
                        weatherArray.getJSONObject(0).getString("description")
                    } else {
                        ""
                    }
                    val iconCode = if (weatherArray.length() > 0) {
                        weatherArray.getJSONObject(0).getString("icon")
                    } else {
                        ""
                    }

                    // Display the location name, temperature, and weather description
                    txtCountry.text = cityName
                    txtCelcius2.text = "${formattedTemperature}°C"
                    txtDescription.text = description.toUpperCase()

                    // Display the additional weather details
                    txtPressureDetails.text = "$pressure hPa"
                    txtHumidityDetails.text = "$humidity%"
                    txtTempDetails.text = "$temperature°C"
                    txtWeatherDetails.text = "${windSpeed} m/s"

                    // Display the weather icon
                    displayWeatherIcon(iconCode)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Error parsing weather information",
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(
                    this,
                    "Error loading weather information",
                    Toast.LENGTH_SHORT
                ).show()
                error.printStackTrace()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun displayWeatherIcon(iconCode: String) {
        // URL for the weather icon
        val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"

        // display the weather icon
        Picasso.get().load(iconUrl).into(imgWeatherImg)
    }



}