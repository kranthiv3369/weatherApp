package com.weatherapp.domain.model

import kotlin.collections.ArrayList

data class WeatherUiModel(
    var city: String = "",
    var weather: String = "",
    var feelsLike: String = "",
    var visibility: String = "",
    var humidity: String = "",
    var clouds: String = "",
    var windSpeed: String = "",
    var pressure: String = "",
    var minTemp: String = "",
    var maxTemp: String = "",
    var sunrise: String = "",
    var sunset: String = "",
    var date: String = "",
    var todayWeatherIcon: ArrayList<Weather>? = null,
)

