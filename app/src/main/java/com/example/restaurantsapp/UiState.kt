package com.example.restaurantsapp

data class UiState (
    val restaurants: List<Restaurant> = listOf(),// restaurantList,
    val selectedRestaurant: Restaurant? = null,
    val coroutineError: Boolean = false,
        )
