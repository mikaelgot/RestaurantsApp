package com.example.restaurantsapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.restaurantsapp.ui.theme.RestaurantsAppTheme
import com.google.gson.annotations.SerializedName


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RestaurantsAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier,//.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Navigation()
                }
            }
        }
    }
}

@Composable
fun RestaurantsScreen(navController: NavController, vm: RestaurantsViewModel) {
    //val vm: RestaurantsViewModel = viewModel()
    val ui = vm.ui
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
    ) {
        item{ Text(text = "Restaurants", style = MaterialTheme.typography.h6)}
        items(ui.restaurants) {
            RestaurantItem(it, vm::toggleFavorite, selectRestaurant = vm::selectRestaurant) { navController.navigate(Screens.DetailsScreen.route) }
        }
    }
    if (ui.coroutineError){
        Toast.makeText(context, "Coroutine Error", Toast.LENGTH_SHORT).show()
        vm.clearCoroutineError()
    }
    if (ui.restaurants.isEmpty()) Busy()
}

@Composable
fun Busy(){
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background.copy(alpha = 0.8f))
    ){
        CircularProgressIndicator()
    }

}

@Composable
fun RestaurantItem(restaurant: Restaurant, toggleFavorite: (Int) -> Unit, selectRestaurant: (Int) -> Unit, navigateToDetails: () -> Unit){
    val icon = if(restaurant.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(5),
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                selectRestaurant(restaurant.id)
                navigateToDetails()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp)
        ) {
            RestaurantIcon(Icons.Filled.Place, id = restaurant.id, modifier = Modifier.weight(0.15f))
            RestaurantDetails(restaurant, modifier = Modifier.weight(0.7f))
            RestaurantIcon(icon, id = restaurant.id, modifier = Modifier.weight(0.15f), onclick = { toggleFavorite(restaurant.id) })
        }
    }
}

@Composable
fun RestaurantIcon(icon: ImageVector, id: Int, modifier: Modifier, onclick: (Int) -> Unit = {}){
    Icon(
        icon,
        contentDescription = null,
        modifier = modifier
            .padding(8.dp)
            .clickable { onclick(id) }
    )
}

@Composable
fun RestaurantDetails(restaurant: Restaurant, modifier: Modifier){
    Column(modifier = modifier) {
        Text(text = restaurant.name, style = MaterialTheme.typography.h6)
        CompositionLocalProvider(
            LocalContentAlpha provides (ContentAlpha.medium)
        ) {
            Text(text = restaurant.description, style = MaterialTheme.typography.body2)
        }

    }
}


//Entity annotation for Room SQLite database
@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey
    @ColumnInfo(name = "r_id") //Room database column
    @SerializedName("r_id")
    val id: Int,
    @ColumnInfo(name ="r_title")
    @SerializedName("r_title")
    val name: String = "No name",
    @ColumnInfo(name = "r_description")
    @SerializedName("r_description")
    val description: String = "No description",
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
