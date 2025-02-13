package com.bl.rustyze.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import com.bl.rustyze.MainActivity
import com.bl.rustyze.R
import com.bl.rustyze.ui.components.VehicleCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

private var mAuth: FirebaseAuth? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, firebaseAuth: FirebaseAuth) {
    var expanded by remember { mutableStateOf(false) }
    val vehiclesLastSeenData = remember { mutableStateOf(emptyList<Map<String, String>>()) }
    val topRatedVehicles = remember { mutableStateOf(emptyList<Map<String, String>>()) }  // Liste des véhicules triés par rustyMeter
    mAuth = FirebaseAuth.getInstance()
    val user: FirebaseUser? = mAuth!!.currentUser
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    if (user != null) {
        // Récupération des véhicules vus dernièrement
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val vehicleIds = document.get("vehiclesLastSeen") as? List<String> ?: emptyList()
                    val vehicles = mutableListOf<Map<String, String>>()

                    vehicleIds.forEach { vehicleId ->
                        val vehicleDocRef = db.collection("vehicles").document(vehicleId)

                        vehicleDocRef.get().addOnSuccessListener { document ->
                            if (document != null) {
                                val fetchedComments =
                                    document.get("comments") as? MutableList<Map<String, *>> ?: mutableListOf()
                                var totalStars = 0
                                var totalComments = 0

                                for (comment in fetchedComments) {
                                    val commentStars = comment["stars"] as? Long
                                    if (commentStars != null && commentStars in 0..5) {
                                        totalStars += commentStars.toInt()
                                        totalComments++
                                    }
                                }

                                val rustyMeterPercentage = if (totalComments > 0) {
                                    (totalStars * 100) / (totalComments * 5)
                                } else {
                                    0
                                }

                                vehicles.add(
                                    mapOf(
                                        "name" to vehicleId,
                                        "rustyMeter" to "$rustyMeterPercentage%"
                                    )
                                )
                                vehiclesLastSeenData.value = vehicles
                            }
                        }.addOnFailureListener { exception ->
                            Log.e("Rustyze", "Error fetching document for vehicleId $vehicleId", exception)
                        }
                    }
                }
            }

        // Récupérer tous les véhicules pour calculer et trier le rustyMeter
        db.collection("vehicles").get()
            .addOnSuccessListener { result ->
                val allVehicles = mutableListOf<Map<String, String>>()

                for (document in result) {
                    val fetchedComments =
                        document.get("comments") as? MutableList<Map<String, *>> ?: mutableListOf()
                    var totalStars = 0
                    var totalComments = 0

                    for (comment in fetchedComments) {
                        val commentStars = comment["stars"] as? Long
                        if (commentStars != null && commentStars in 0..5) {
                            totalStars += commentStars.toInt()
                            totalComments++
                        }
                    }

                    val rustyMeterPercentage = if (totalComments > 0) {
                        (totalStars * 100) / (totalComments * 5)
                    } else {
                        0
                    }

                    allVehicles.add(
                        mapOf(
                            "name" to document.id,
                            "rustyMeter" to "$rustyMeterPercentage%"
                        )
                    )
                }

                // Trier les véhicules par rustyMeter
                allVehicles.sortByDescending { it["rustyMeter"]?.removeSuffix("%")?.toIntOrNull() ?: 0 }
                topRatedVehicles.value = allVehicles
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rustyze") },
                actions = {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopEnd)
                    ) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.navProfile)) },
                                onClick = {
                                    expanded = false
                                    navController.navigate("profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.logout)) },
                                onClick = {
                                    expanded = false
                                    firebaseAuth.signOut()
                                    Toast.makeText(navController.context, "Logged out", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(navController.context, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(navController.context, intent, null)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { navController.navigate("home") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Home") },
                    label = { Text(stringResource(id = R.string.navHome)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("search") },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text(stringResource(id = R.string.navSearch)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("comments") },
                    icon = { Icon(Icons.Default.Email, contentDescription = "my comments") },
                    label = { Text(stringResource(id = R.string.navComments)) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Top Rated Section
            item {
                Text(
                    text = stringResource(id = R.string.topRated),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    content = {
                        LazyRow {
                            // Display top 10 rated vehicles
                            items(topRatedVehicles.value.take(10)) { vehicle ->
                                val vehicleId = vehicle["name"] ?: "Unknown"
                                val rustyMeter = vehicle["rustyMeter"] ?: "Unknown"
                                TopRatedCard(
                                    rank = topRatedVehicles.value.indexOf(vehicle) + 1,
                                    vehicleId = vehicleId,
                                    rustyMeter = rustyMeter,
                                    imageRes = R.drawable.ic_launcher_foreground // Replace with vehicle image
                                )
                            }
                        }
                    }
                )
            }

            // Seen Recently Section
            item {
                Text(
                    text = stringResource(id = R.string.seenRecently),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(vehiclesLastSeenData.value.takeLast(3).size) { index ->
                val vehicle = vehiclesLastSeenData.value.takeLast(3)[index]
                VehicleCard(
                    modifier = Modifier.clickable {
                        navController.navigate("details/${vehicle["name"]?.split(" ")?.get(0)}/${vehicle["name"]?.split(" ")?.get(1)}")
                    },
                    vehicleName = vehicle["name"] ?: "Unknown",
                    rustyMeter = vehicle["rustyMeter"] ?: "Unknown"
                )
            }
        }
    }
}

@Composable
fun TopRatedCard(rank: Int, vehicleId: String, rustyMeter: String, imageRes: Int) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Vehicle Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
            )
            Text(
                text = vehicleId,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
            )
            Text(
                text = rustyMeter,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
            )
        }
    }
}

