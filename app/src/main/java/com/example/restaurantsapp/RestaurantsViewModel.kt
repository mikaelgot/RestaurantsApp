package com.example.restaurantsapp

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.UnknownHostException

class RestaurantsViewModel(/*private val stateHandle: SavedStateHandle*/): ViewModel(){
    //restInterface variable for Retrofit
    private var restInterface: RestaurantsApiService
    //error handler for all coroutines automatically, so no need to put them in try/catch blocks
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        //_ui.value = ui.copy(coroutineError = true)
        exception.printStackTrace()
    }
    //Database Dao
    private var restaurantsDao = RestaurantsDb.getDaoInstance(RestaurantsApplication.getAppContext())

    private val _ui = mutableStateOf(UiState())
    val ui get() = _ui.value

    init {
        Log.i("MyInfo", "init block started")

        //Instantiate Retrofit builder object
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://kickstartandroidrestaurants-default-rtdb.europe-west1.firebasedatabase.app/")
            .build()
        restInterface = retrofit.create(RestaurantsApiService::class.java)
        getRestaurants()
        Log.i("MyInfo", "init block ended")
    }

    fun clearCoroutineError(){
        _ui.value = ui.copy(coroutineError = false)
    }

    //the onCleared is provided as default by the viewModel to clear it when the calling
    // Activity/Fragment/Composable is destroyed
    //Here we override it to add restaurantsCall.cancel() so that the Api call is cancelled when
    // the viewmodel is cleared to avoid ongoing calls and memory leaks
    override fun onCleared() {
        super.onCleared()
        //restaurantsCall.cancel()
    }

    fun getRestaurants() {
        //Don't forget Internet permission in manifest!!!!!
        viewModelScope.launch(errorHandler) {
            val restaurants = getAllRestaurants()
            Log.i("MyInfo", "getRestaurants: restaurants: $restaurants ")
            _ui.value = ui.copy(restaurants = restaurants)
            //restoreSelections()
        }
    }

    /** The most important part:
     * 1. Switching to Dispatchers.IO because it is called from getRestaurants which runs inside
     * vieModelScope whose default is Dispatchers.Main, which we actually like because it is performing
     * UI job, but getAllRestaurants is a network call so we switch to Dispatchers.IO
     *
     * 2.We try to get the Restaurant list from internet (using Retrofit restInterface call)
     * but if we catch an exception of type UnknownHostException, UnknownHostException or UnknownHostException
     * we get the restaurant list offline from the storeR sqLite Room database (through restaurantsDao access object)
     * If the exception is of different kind we just throw it (and it is caught by CoroutineExceptionHandler
     * errorHandler declare at the start of the viewModel.
     * **/
    /*private suspend fun getAllRestaurants(): List<Restaurant>{
        Log.i("MyInfo", "getAllRestaurants invoked")
        return withContext(Dispatchers.IO){
            try {
                val restaurants = restInterface.getRestaurants()
                Log.i("MyInfo", "getAllRestaurants: restaurants from internet: $restaurants ")
                restaurantsDao.addAll(restaurants)
                Log.i("MyInfo", "getAllRestaurants: Written results to local database ")

                return@withContext restaurants
            }
            catch (e: Exception){
                when(e){
                    is UnknownHostException, is UnknownHostException, is UnknownHostException ->{
                        return@withContext  restaurantsDao.getAll()
                    }
                    else -> throw e
                }
            }
        }
    }*/

    /** Refactored version in order to have Single Source of Truth, which is going to be the
     * database. If we are connected to the internet, we download the data and save it in the database
     * but even then the UI receives data from the database and not the internet directly.
     * So the SSoT is the Room database!
     */
    private suspend fun getAllRestaurants(): List<Restaurant>{
        Log.i("MyInfo", "getAllRestaurants invoked")
        return withContext(Dispatchers.IO){
            try {
                val remoteRestaurants = restInterface.getRestaurants()
                //We can't add them to the database right away, to update it, because this way all the
                // favorited would turn to false, the only source who knows about the favorites is the local DB
                //So, we first query the favorited restaurants from the database
                val favRestaurants = restaurantsDao.getAllFavorited()
                //Next we add to the DB the list from internet (without favorited)
                restaurantsDao.addAll(remoteRestaurants)
                //Finally we update the DB with the favorited list we saved above mapped as Partial Restaurants
                //We use partial so to not interfere with the rest of data like name, description etc.
                //These might have changed in the meanwhile on the internet, who knows?
                //We only care about what's local and user-dependent (i.e the favorite ones)
                val partialFavRestaurants = favRestaurants.map { PartialRestaurant(id = it.id, isFavorite = it.isFavorite) }
                restaurantsDao.updateAll(partialFavRestaurants)
                Log.i("MyInfo", "getAllRestaurants: Written results to local database ")
            }
            catch (e: Exception){
                when(e) {
                    is UnknownHostException, is UnknownHostException, is UnknownHostException -> {
                        //See if the database is empty and if it is throw exception
                        if(restaurantsDao.getAll().isEmpty()) throw  Exception(
                            "Something went wrong. The database is empty"
                        )
                    }
                    else -> throw e
                }
            }
            return@withContext restaurantsDao.getAll()
        }
    }

    fun toggleFavorite(id: Int){    //toggle isFavorite in UI
        val restaurants = ui.restaurants.toMutableList()
        val index = restaurants.indexOfFirst { it.id == id }
        val oldRestaurant = restaurants[index]
        val newRestaurant = oldRestaurant.copy(isFavorite = !oldRestaurant.isFavorite)
        restaurants[index] = newRestaurant
        //storeSelection(newRestaurant)

        //Updating the UI here is wrong because it would violate the SSoT principle
        //If we want to have the database as SSoT, we first have to update the database and then
        // fetch the data from there again to UI
        //_ui.value = ui.copy(restaurants = restaurants)
        viewModelScope.launch {
            //updating the Room database as well
            //I'm passing the old isFavorite value as argument, could also pass the new one,
            // depends on the toggleFavoriteRestaurant structure
            toggleFavoriteRestaurant(id, oldRestaurant.isFavorite)
            val restaurantsFromDB = fetchRestaurantsFromDBtoUI()
            _ui.value = ui.copy(restaurants = restaurantsFromDB)
        }
        Log.i("MyInfo", "toggleFavorite, id = $id, restaurants: ${restaurants.map{it.isFavorite}}")
    }

    private suspend fun toggleFavoriteRestaurant(id: Int, oldValue: Boolean) {  //toggle isFavorite in Room database
        withContext(Dispatchers.IO){
            restaurantsDao.update(PartialRestaurant(id = id, isFavorite = !oldValue))
        }
    }
    private suspend fun fetchRestaurantsFromDBtoUI(): List<Restaurant> {  //toggle isFavorite in Room database
        return withContext(Dispatchers.IO){
           restaurantsDao.getAll()
        }
    }

    /**NOT USED ANYMORE :Store and Restore were used with the SavedStateHandle for
     * retaining the favorite restaurants in case of a system-initiated process death.
     * Now that we implemented a Room database and the isFavorite attribute is stored in the local database
     * everytime it is changed, we no longer need the saved state
     */
    /*private fun storeSelection(restaurant: Restaurant) {
        val savedFavoriteIds = stateHandle
            .get<List<Int>?>(FAVORITES)
            .orEmpty().toMutableList()
        if (restaurant.isFavorite) savedFavoriteIds.add(restaurant.id)
        else savedFavoriteIds.remove(restaurant.id)
        stateHandle[FAVORITES] = savedFavoriteIds
        Log.i("MyInfo", "storeSelection: Selections stored, savedFavorites: $savedFavoriteIds")
    }
    companion object{
        const val FAVORITES = "favorites"
    }
    private fun restoreSelections(){
        val savedFavoriteIds = stateHandle
            .get<List<Int>?>(FAVORITES)
            .orEmpty().toMutableList()
        Log.i("MyInfo", "restoreSelections, savedFavorites: $savedFavoriteIds")
        savedFavoriteIds.forEach { toggleFavorite(it) }
    }*/

    fun selectRestaurant(id: Int){
        viewModelScope.launch {
            val selectedRestaurant = withContext(Dispatchers.IO) {
                restaurantsDao.getRestaurantById(id)
            }
            _ui.value = ui.copy(selectedRestaurant = selectedRestaurant )
        }
    }

}