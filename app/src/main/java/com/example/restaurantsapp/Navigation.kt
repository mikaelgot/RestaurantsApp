package com.example.restaurantsapp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun Navigation(){
    val navController = rememberNavController()
    val vm: RestaurantsViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screens.RestaurantsScreen.route){
        composable(route = Screens.RestaurantsScreen.route){
            RestaurantsScreen(navController = navController, vm = vm)
        }
        composable(route = Screens.DetailsScreen.route){
            DetailsScreen(navController = navController, vm = vm)
        }
    }
}

sealed class Screens(val route: String){
    object RestaurantsScreen: Screens("restaurantsscreen")
    object DetailsScreen: Screens("detailsscreen")
}
