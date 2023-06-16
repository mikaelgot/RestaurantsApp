package com.example.restaurantsapp

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun DetailsScreen(navController: NavController, vm: RestaurantsViewModel) {
    //val vm: DetailsViewModel = viewModel()
    val ui = vm.ui
    val context = LocalContext.current

    
    Column() {
        ui.selectedRestaurant?.let { RestaurantItem(restaurant = it, toggleFavorite = { }, selectRestaurant = {}, navigateToDetails = {}) }
    }

    //We may need DI for this because we have 2 viewmodels

}