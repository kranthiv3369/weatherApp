package com.weatherapp.ui.activity

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.android.gms.location.*
import com.weatherapp.domain.model.WeatherUiModel
import com.weatherapp.ui.theme.ComposeMVVMWeatherAppTheme
import com.weatherapp.ui.theme.backgroundColor
import com.weatherapp.ui.theme.textColor
import com.weatherapp.ui.theme.weeklyItemBackgroundColor
import com.weatherapp.utils.capitalize
import com.weatherapp.utils.getCityName
import com.weatherapp.utils.getWeatherIcon
import com.weatherapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.trimSubstring

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeMVVMWeatherAppTheme {
                AskCheckLocationPermission()
            }
        }
    }

}

@Composable
fun AskCheckLocationPermission(){

    val context = LocalContext.current
    var locationName:String? = null
    val mainViewModel: MainViewModel = viewModel();
    // Create a permission launcher
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted, update the location
                    mainViewModel.getCurrentLocation(context) { lat, long ->
                        locationName = getCityName(context = context, lat = lat, long = long)
                        mainViewModel.fetchWeather(locationName!!)
                    }
                }
            })

    if (mainViewModel.hasLocationPermission(context)) {
        // Permission already granted, update the location
        mainViewModel.getCurrentLocation(context) { lat, long ->
            locationName = getCityName(context = context, lat = lat, long= long)
            mainViewModel.fetchWeather(locationName!!)
        }
    } else {
        // Request location permission
        SideEffect{
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    CurrentWeatherHeader(mainViewModel = mainViewModel, locationName = locationName)
}

@Composable
fun CurrentWeatherHeader(mainViewModel: MainViewModel?, locationName: String?) {

    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth().background(backgroundColor)) {
        when (val state = mainViewModel?.uiState?.collectAsState()?.value) {
            is MainViewModel.WeatherUiState.Empty -> Text(
                text = stringResource(com.weatherapp.R.string.no_data_available),
                modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxHeight().wrapContentHeight(align = Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 22.sp)
            )

            is MainViewModel.WeatherUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                WeatherLoadingAnimation()
            }

            is MainViewModel.WeatherUiState.Error -> ErrorDialog(state.message)
            is MainViewModel.WeatherUiState.Loaded -> WeatherLoadedScreen(state.data, mainViewModel, currentLocation = locationName)
            else -> {}
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun WeatherLoadedScreen(data: WeatherUiModel, mainViewModel: MainViewModel?, currentLocation:String? ) {

    Spacer(modifier = Modifier.size(10.dp))
    var query: String by rememberSaveable { mutableStateOf("") }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        value = query,
        onValueChange = { newText ->
            query = newText
                        },
        maxLines = 1,
        textStyle = TextStyle(fontWeight = FontWeight.Light, fontSize = 20.sp, color = Color.Black),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        placeholder = {
            Text(text = currentLocation?:"Enter city name", style =
            TextStyle(fontWeight = FontWeight.Light, fontSize = 20.sp, color = Color.Gray))
                      },

        keyboardActions = KeyboardActions(
            onSearch = {
                mainViewModel?.fetchWeather(query.trimSubstring())
            }
        ),
    )

    Text(
        text = data.city,
        modifier = Modifier.padding(start = 20.dp),
        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
    )
    Text(
        text = data.date,
        modifier = Modifier.padding(start = 20.dp),
        style = TextStyle(fontSize = 16.sp, color = textColor)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterVertically),
            backgroundColor = weeklyItemBackgroundColor,
            shape = RoundedCornerShape(40.dp),
            elevation = 0.dp,
        ) {
            GlideImage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                model = getWeatherIcon(data.todayWeatherIcon?.get(0)?.description?:""),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(com.weatherapp.R.string.image),
            )
        }
        Text(
            text = data.weather,
            modifier = Modifier.padding(start = 10.dp),
            style = TextStyle(
                fontWeight = FontWeight.Bold, fontSize = 70.sp, color = textColor
            )
        )
        Text(
            text = "°C",
            modifier = Modifier.padding(top = 20.dp),
            style = TextStyle(
                fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColor
            )
        )
        Text(
            text = capitalize(data.todayWeatherIcon?.get(0)?.description?:""),
            modifier = Modifier
                .padding(start = 20.dp)
                .align(Alignment.CenterVertically),
            style = TextStyle(
                fontWeight = FontWeight.Bold, fontSize = 25.sp, color = textColor,
            )
        )
    }
    Spacer(modifier = Modifier.size(15.dp))
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DailyItem(
                com.weatherapp.R.drawable.temprature, data.feelsLike, "Feels Like"
            )
            DailyItem(
                com.weatherapp.R.drawable.visibility, data.visibility, "Visibility"
            )
        }
        Spacer(modifier = Modifier.size(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DailyItem(
                com.weatherapp.R.drawable.humidity, data.humidity, "Humidity"
            )
            DailyItem(
                com.weatherapp.R.drawable.wind_speed, data.windSpeed, "Wind Speed"
            )
            DailyItem(
                com.weatherapp.R.drawable.pressure, data.pressure, "Air Pressure"
            )
        }
    }
    Spacer(modifier = Modifier.size(25.dp))

    if(mainViewModel?.searchCitiesLIst?.size!! > 0)
        ListItem(mainViewModel = mainViewModel)
}

@Composable
fun DailyItem(icDay: Int, data: String, stringText: String) {
    Card(
        modifier = Modifier.size(100.dp),
        backgroundColor = weeklyItemBackgroundColor,
        shape = RoundedCornerShape(15.dp),
        elevation = 0.dp
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = icDay),
                contentDescription = stringResource(com.weatherapp.R.string.day_temp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(25.dp)
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = data,
                style = TextStyle(fontSize = 16.sp, color = textColor),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 5.dp)
            )

            Text(
                text = stringText,
                style = TextStyle(fontSize = 14.sp, color = textColor),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ErrorDialog(message: String) {
    val openDialog = remember { mutableStateOf(true) }
    if (openDialog.value) {
        AlertDialog(onDismissRequest = {
            openDialog.value = false
        }, title = {
            Text(text = stringResource(com.weatherapp.R.string.problem_occurred))
        }, text = {
            Text(message)
        }, confirmButton = {
            openDialog.value = false
        })
    }
}

@Composable
fun WeatherLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()

    // Create an animation state for the scale factor
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000 // Total duration of one zoom in/out cycle
                0.5f at 500 // Zoom in at the halfway point (1000ms)
            },
            repeatMode = RepeatMode.Reverse
        )
    )

    // Apply the zoom effect to the image
    Image(
        painter = painterResource(id = com.weatherapp.R.drawable.weather), // Replace with your icon
        contentDescription = null,
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ListItem(
    mainViewModel: MainViewModel?
) {

    Text(
        text = "Searched cities:",
        style = TextStyle(fontSize = 20.sp, color = Color.DarkGray),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
    )
    LazyColumn{

        itemsIndexed(mainViewModel?.searchCitiesLIst!!){ _, item ->
            
            Card(
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                backgroundColor = Color.Transparent,
                onClick = {
                    mainViewModel.fetchWeather(item.trimSubstring())
                },
                elevation = 1.dp,
                modifier = Modifier.padding(bottom = 6.dp, start = 10.dp, end = 10.dp).fillMaxWidth()
            ){

                Text(
                    text = item,
                    style = TextStyle(fontSize = 20.sp, color = Color.Black),
                    modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
                )
            }

        }
    }
}