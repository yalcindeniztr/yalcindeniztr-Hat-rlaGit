package com.example.ui.tabs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LifeAssistantViewModel
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkScreen(
    viewModel: LifeAssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val parkedCarLat by viewModel.parkedCarLat.collectAsStateWithLifecycle()
    val parkedCarLng by viewModel.parkedCarLng.collectAsStateWithLifecycle()
    val parkedCarTime by viewModel.parkedCarTime.collectAsStateWithLifecycle()
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    fun saveParkLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.saveParkedCarLocation(location.latitude.toString(), location.longitude.toString())
                    Toast.makeText(context, "Araba konumu kaydedildi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Konum alınamadı, GPS açık mı?", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Konum izni gerekli.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            saveParkLocation()
        } else {
            Toast.makeText(context, "Konum izni reddedildi.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Araba Park Yerim", fontWeight = FontWeight.Black, color = Slate900) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F2ED))
            )
        },
        containerColor = Color(0xFFF5F2ED)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (parkedCarLat != null && parkedCarLng != null) {
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    elevation = 4.dp,
                    contentPadding = 24.dp,
                    glowColor = Color(0xFFFFEA00)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aracınız park halinde!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Slate900)
                        val timeStr = if (parkedCarTime != null) {
                            SimpleDateFormat("dd MMM yyyy - HH:mm", Locale("tr")).format(Date(parkedCarTime!!))
                        } else ""
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Park Zamanı: $timeStr", color = Slate700, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=$parkedCarLat,$parkedCarLng&mode=w")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try { context.startActivity(mapIntent) } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Yol Tarifi Al", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = { 
                                viewModel.clearParkedCarLocation()
                                Toast.makeText(context, "Park yeri silindi", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Konumu Temizle", color = Color.Red)
                        }
                    }
                }
            } else {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Kayıtlı bir park yeriniz yok.", fontSize = 16.sp, color = Slate700)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            saveParkLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEA00))
                ) {
                    Text("Şu Anki Konumu Park Olarak Kaydet", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
