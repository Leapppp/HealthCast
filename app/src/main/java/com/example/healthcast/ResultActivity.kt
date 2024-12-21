package com.example.healthcast

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthcast.api.ApiRest
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.ActivityResultBinding
import com.example.healthcast.fragment.ScanFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var binding: ActivityResultBinding
    lateinit var context: Context

    var latitude = 0.0
    var longitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        context = this@ResultActivity

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        latitude = intent.getDoubleExtra("latitude", -6.200000)
        longitude = intent.getDoubleExtra("latitude", 106.816666)

        if(latitude == 0.0 && longitude == 0.0){
            latitude = -6.200000
            longitude = 106.816666
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        requestLocationPermission()

        setContentView(binding.root)

        getWheather()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission
            ActivityCompat.requestPermissions(
                this@ResultActivity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this@ResultActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                    getWheather()
                    Log.d("Location", "Latitude: $latitude, Longitude: $longitude")
                } ?: {
                    getWheather()
                    Log.e("Location", "Unable to get location")
                }
            }.addOnFailureListener { e ->
                Log.e("Location", "Error getting location: ${e.message}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getCurrentLocation()
            } else {
                // Permission denied
                Toast.makeText(
                    context,
                    "Location permission is required to get your current location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getWheather() {
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)
        val mApiClient: ApiRest = UtilsApi.getAPIServiceWheather()
        mApiClient.getWheater(latitude.toString(), longitude.toString())
            .enqueue(object : Callback<JsonObject?> {
                @RequiresApi(Build.VERSION_CODES.O)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    loading.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val json = JSONObject(response.body().toString())
                            val name = json.getString("name")
                            val weather = json.getJSONArray("weather")
                            val main = json.getJSONObject("main")

                            if (weather.length() > 0) {
                                val weatherObject = weather.getJSONObject(0)
                                val description = weatherObject.getString("description")
                                binding.tvCuaca.text = description
                            }

                            binding.tvLocation.text = name
                            val kelvin = main.getDouble("feels_like")
                            val celcius = kelvin - 273.15
                            binding.tvSuhu.text = celcius.toInt().toString() + "°C"

                            binding.tvDateNumber.text = getCurrentDay()
                            binding.tvCurrentDate.text = getCurrentDateFormatted()

                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(context, "Kesalahan Parsing Data", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(context, "Gagal Upload Data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.i("debug", "onFailure: ERROR > $t")
                    Toast.makeText(context, "Keneksi Error", Toast.LENGTH_SHORT).show()
                    loading.dismiss()
                }
            })
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateFormatted(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)
        return currentDate.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDay(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd", Locale.ENGLISH)
        return currentDate.format(formatter)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}