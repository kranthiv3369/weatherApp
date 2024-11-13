package com.weatherapp.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.weatherapp.R
import com.weatherapp.domain.model.WeatherUiModel
import com.weatherapp.domain.repository.WeatherRepository
import com.weatherapp.utils.*
import com.weatherapp.utils.Constants.DATABASE_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository,
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Empty)
    val uiState: StateFlow<WeatherUiState> = _uiState
    var prefs: SharedPreferences? = null
    var searchCitiesLIst: MutableList<String> = mutableListOf()

    init {
        prefs = applicationContext.getSharedPreferences(DATABASE_NAME, Context.MODE_PRIVATE)
        val listItems:List<String>? = getList(prefs);
        if (listItems != null)
            searchCitiesLIst =  listItems as MutableList<String>
    }

    public fun fetchWeather(name: String) {
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.fetchWeather(cityName = name)

                if (response.cod == 200) {

                    val cityName = response.name?:""
                    saveSearchedCity(cityName)

                    _uiState.value = WeatherUiState.Loaded(
                        WeatherUiModel(
                            city = cityName,
                            weather = "${fahrenheitToCelsius(response.main?.temp?:0.0)}",
                            feelsLike = "${fahrenheitToCelsius(response.main?.feelsLike?:0.00)}°C",
                            visibility = "${metersToKilometers(response.visibility?:0)} km",
                            clouds =  "${response.clouds?.all}",
                            humidity = "${response.main?.humidity}%",
                            windSpeed = "${response.wind?.speed} m/s",
                            pressure = "${response.main?.pressure} hPa",
                            minTemp = "${fahrenheitToCelsius(response.main?.tempMin?:0.00)}",
                            maxTemp = "${fahrenheitToCelsius(response.main?.tempMax?:0.00)}",
                            sunset = getDateTime(response.sys?.sunset?:0).toString(),
                            sunrise = getDateTime(response.sys?.sunrise?:0).toString(),
                            date = getDateTime(response.dt?:0).toString(),
                            todayWeatherIcon = response.weather
                        )
                    )
                }else{
                    _uiState.value = WeatherUiState.Error(
                        "city not found"
                    )
                }

            } catch (ex: Exception) {
                if (ex is HttpException && ex.code() == 429) {
                    onQueryLimitReached()
                } else {
                    onErrorOccurred()
                }
            }
        }
    }

    private fun onQueryLimitReached() {
        _uiState.value = WeatherUiState.Error(
            applicationContext.getString(R.string.query_limit_reached)
        )
    }

    private fun onErrorOccurred() {
        _uiState.value = WeatherUiState.Error(
            applicationContext.getString(R.string.something_went_wrong)
        )
    }

    sealed class WeatherUiState {
        object Empty : WeatherUiState()
        object Loading : WeatherUiState()
        class Loaded(val data: WeatherUiModel) : WeatherUiState()
        class Error(val message: String) : WeatherUiState()
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentLocation(context: Context, callback: (Double?, Double?) -> Unit) {

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val long = location.longitude
                        callback(lat, long)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle location retrieval failure
                    exception.printStackTrace()
                }
        }

    }

    fun saveSearchedCity(cityName: String){
        if(!searchCitiesLIst.contains(cityName)) searchCitiesLIst.add(cityName)

        setList(prefs, searchCitiesLIst)
    }

}