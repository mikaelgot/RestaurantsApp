package com.example.restaurantsapp

import androidx.room.*

@Dao
interface RestaurantsDao {
    //to get the list of all restaurants from the database
    @Query("SELECT * FROM restaurants")
    suspend fun getAll() : List<Restaurant>

    //to save the list of all restaurants to the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(restaurants: List<Restaurant>)

    //to update a single entity in the database
    @Update(entity = Restaurant::class)
    suspend fun update(partialRestaurant: PartialRestaurant)

    //To update multiple entities in the database
    @Update(entity = Restaurant::class)
    suspend fun updateAll(partialRestaurants: List<PartialRestaurant>)

    //to get the list of restaurants that have isFavorite == true
    @Query("SELECT * FROM restaurants WHERE is_favorite = 1")
    suspend fun getAllFavorited(): List<Restaurant>

    //get single entity by Primary key (note the :id notation)
    @Query("SELECT * FROM restaurants WHERE r_id = :id")
    suspend fun getRestaurantById(id: Int): Restaurant
}