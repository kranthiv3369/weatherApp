package com.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weatherapp.R
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@SuppressLint("SimpleDateFormat")
fun getDateTime(s: Int): String? {
    try {
        val sdf = SimpleDateFormat("MM/dd/yyyy")
        val netDate = Date(s.toLong() * 1000)
        return sdf.format(netDate)
    } catch (e: Exception) {
        return e.toString()
    }
}

fun fahrenheitToCelsius(temperatureInFahrenheit: Double): Int {
    val celsius = (temperatureInFahrenheit - 32) * 5 / 9
    return celsius.toInt()
}

fun metersToKilometers(meters: Int): Int {
    return meters / 1000
}

fun capitalize(capString: String): String {
    val capBuffer = StringBuffer()
    val capMatcher =
        Pattern.compile("([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(capString)
    while (capMatcher.find()) {
        capMatcher.appendReplacement(
            capBuffer,
            (capMatcher.group(1)?.uppercase()) + (capMatcher.group(2)
                ?.uppercase())
        )
    }
    return capMatcher.appendTail(capBuffer).toString()
}

fun getWeatherIcon(weather: String): Int {
    return when (weather) {
        "clear sky" -> R.drawable.clear_sky
        "few clouds" -> R.drawable.few_clouds
        "shower rain" -> R.drawable.shower_rain
        "thunderstorm" -> R.drawable.thunderstorm
        "broken clouds" -> R.drawable.broken_clouds
        "overcast clouds" -> R.drawable.scattered_clouds
        "scattered clouds" -> R.drawable.scattered_clouds
        "rain" -> R.drawable.rain
        "snow" -> R.drawable.snow
        "mist" -> R.drawable.mist
        else -> R.drawable.few_clouds
    }
}

fun getCityName(context: Context, lat: Double?,long: Double?): String {
    val geoCoder = Geocoder(context, Locale.getDefault())
    val addresses = geoCoder.getFromLocation(lat?:0.0,long?:0.0,1)
    Log.i("TAG", "getCityName: ${addresses?.get(0)?.getLocality()?:""}")
    return  addresses?.get(0)?.getLocality()?:"";

}

fun setList(sharedPreferences: SharedPreferences?, list: List<String>?) {
    val editor = sharedPreferences?.edit()
    val gson = Gson()
    val json = gson.toJson(list)
    editor?.putString(Constants.SEARCH_ITEMS, json)
    editor?.apply()
}

fun getList(sharedPreferences: SharedPreferences?): List<String>? {
    val serializedObject: String? = sharedPreferences?.getString(Constants.SEARCH_ITEMS, null)
    val gson = Gson()
    val type: Type = object : TypeToken<List<String?>?>() {}.type
    return gson.fromJson<List<String>>(serializedObject, type)
}