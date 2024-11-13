package com.weatherapp.network

import com.weatherapp.domain.model.WeatherResponse
import com.weatherapp.utils.Constants
import retrofit2.http.GET
import retrofit2.http.Query

interface NetworkingService {

    @GET("data/2.5/weather?")
    suspend fun fetchWeather(
        @Query("q") cityName: String,
        @Query("appid") appid: String = Constants.APP_ID
    ): WeatherResponse

}