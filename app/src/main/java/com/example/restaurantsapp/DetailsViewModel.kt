package com.example.restaurantsapp

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetailsViewModel(private val stateHandle: SavedStateHandle): ViewModel() {
    //restInterface variable for Retrofit
    private var restInterface: RestaurantsApiService

    //error handler for all coroutines automatically, so no need to put them in try/catch blocks
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    private val _ui = mutableStateOf<Restaurant?>(null)
    val ui get() = _ui.value

    init {
        Log.i("MyInfo", "init block started")

        //Instantiate Retrofit builder object
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://kickstartandroidrestaurants-default-rtdb.europe-west1.firebasedatabase.app/")
            .build()
        restInterface = retrofit.create(RestaurantsApiService::class.java)
        getRestaurant()
    }
    fun getRestaurant() {
        //Don't forget Internet permission in manifest!!!!!
        viewModelScope.launch(errorHandler) {
            val restaurantDetails = getRemoteRestaurant(2)
            _ui.value = restaurantDetails.values.first()
        }
    }

    suspend fun getRemoteRestaurant(id: Int): Map<String, Restaurant>{
        return withContext(Dispatchers.IO){
            restInterface.getRestaurant(id)
        }
    }
}