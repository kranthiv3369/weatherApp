package com.weatherapp.domain.repository

import com.weatherapp.domain.model.WeatherResponse
import com.weatherapp.network.NetworkingService
import javax.inject.Inject

class WeatherRepository @Inject constructor(private val networkingService: NetworkingService) {

    suspend fun fetchWeather(cityName: String): WeatherResponse =
        networkingService.fetchWeather(cityName = cityName)

}