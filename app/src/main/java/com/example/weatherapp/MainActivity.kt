package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null

    private lateinit var tvMain: TextView
    private lateinit var tvMainDescription: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvMin: TextView
    private lateinit var tvMax: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvName: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvSunriseTime: TextView
    private lateinit var tvSunsetTime: TextView
    private lateinit var ivMain: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iniView()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
//            Toast.makeText(this, "Your location provider is already turned ON", Toast.LENGTH_SHORT).show()
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "Your have denied location permission. Please enable them as it is mandatory for the app to work.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }

    }

    private fun iniView() {
        tvMain = findViewById(R.id.tvMain)
        tvMainDescription = findViewById(R.id.tvMainDescription)
        tvTemp = findViewById(R.id.tvTemp)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvMin = findViewById(R.id.tvMin)
        tvMax = findViewById(R.id.tvMax)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvName = findViewById(R.id.tvName)
        tvCountry = findViewById(R.id.tvCountry)
        tvSunriseTime = findViewById(R.id.tvSunriseTime)
        tvSunsetTime = findViewById(R.id.tvSunsetTime)
        ivMain = findViewById(R.id.ivMain)
    }

    private fun isLocationEnabled(): Boolean {
        // this provides access to the system location services
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val mLastLocation: Location = locationResult.lastLocation!!
            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude
            Log.i("Current_Location", "Location: lat = $latitude lon = $longitude")

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double = 34.7854, longitude: Double = 33.456) {
        if (Constants.isNetworkAvailable(this)) {

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            showCustomProgressDialog()

            val listCall: Call<WeatherResponse> = service.getWeather(
                lat = latitude,
                lon = longitude,
                units = Constants.METRIC_UNIT,
                appid = Constants.API_ID
            )

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    hideProgressDialog()
                    if (response!!.isSuccess) {
                        val weatherList: WeatherResponse = response.body()
                        setupUI(weatherList)
                    } else {
                        when (response.code()) {
                            400 -> {
                                Log.e("RESPONSE_ERROR", "Bad Connection")
                            }
                            404 -> {
                                Log.e("RESPONSE_ERROR", "Not Found")
                            }
                            else -> {
                                Log.e("RESPONSE_ERROR", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    hideProgressDialog()
                    Log.e("RESPONSE_FAILURE", t!!.message.toString())
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        mProgressDialog?.let {
            it.dismiss()
        }
    }

    private fun setupUI(weatherList: WeatherResponse) {
        for (i in weatherList.weather.indices) {

            Log.i("Weather_Name", weatherList.weather.toString())

            tvMain.text = weatherList.weather[i].main
            tvMainDescription.text = weatherList.weather[i].description
            tvTemp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            tvHumidity.text = weatherList.main.humidity.toString() + " per cent"
            tvMin.text = weatherList.main.temp_min.toString() + " min"
            tvMax.text = weatherList.main.temp_max.toString() + " max"
            tvSpeed.text = weatherList.wind.speed.toString()
            tvName.text = weatherList.name
            tvCountry.text = weatherList.sys.country
            tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
            tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())

            // Here we update the main icon
            when (weatherList.weather[i].icon) {
                "01d" -> ivMain.setImageResource(R.drawable.sunny)
                "02d" -> ivMain.setImageResource(R.drawable.cloud)
                "03d" -> ivMain.setImageResource(R.drawable.cloud)
                "04d" -> ivMain.setImageResource(R.drawable.cloud)
                "04n" -> ivMain.setImageResource(R.drawable.cloud)
                "10d" -> ivMain.setImageResource(R.drawable.rain)
                "11d" -> ivMain.setImageResource(R.drawable.storm)
                "13d" -> ivMain.setImageResource(R.drawable.snowflake)
                "01n" -> ivMain.setImageResource(R.drawable.cloud)
                "02n" -> ivMain.setImageResource(R.drawable.cloud)
                "03n" -> ivMain.setImageResource(R.drawable.cloud)
                "10n" -> ivMain.setImageResource(R.drawable.cloud)
                "11n" -> ivMain.setImageResource(R.drawable.rain)
                "13n" -> ivMain.setImageResource(R.drawable.snowflake)
            }
        }
    }

    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.manu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.actionRefresh -> {
                requestLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
