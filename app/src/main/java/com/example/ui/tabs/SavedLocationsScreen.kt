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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Place
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
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(viewModel: LifeAssistantViewModel) {
    val context = LocalContext.current
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var newLocationName by remember { mutableStateOf("") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchAndSaveLocation(name: String) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.addSavedLocation(
                        name = name,
                        lat = location.latitude,
                        lng = location.longitude
                    )
                    Toast.makeText(context, "Konum kaydedildi!", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                    newLocationName = ""
                } else {
                    Toast.makeText(context, "Konum alınamadı.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Konum izni reddedildi.", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchAndSaveLocation(newLocationName)
        } else {
            Toast.makeText(context, "Konum izni verilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("Yeni Konum Kaydet", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text("Konum Adı (Örn: Arabam, Ev, Park)", color = Color.DarkGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = Color(0xFF40E0D0),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF40E0D0),
                        unfocusedLabelColor = Color.DarkGray
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newLocationName.isNotBlank()) {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            fetchAndSaveLocation(newLocationName)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    } else {
                        Toast.makeText(context, "Lütfen bir isim girin", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40E0D0))) {
                    Text("GPS ile Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("İptal") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF40E0D0))
                .clickable { showAddDialog = true }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Şu Anki Konumu Kaydet", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (savedLocations.isEmpty()) {
                item {
                    Text("Henüz kaydedilmiş bir konumunuz yok.", color = Color.Black, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(savedLocations) { location ->
                    EmbossedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                        contentPadding = 16.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF40E0D0))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = location.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                                }
                                IconButton(onClick = { viewModel.deleteSavedLocation(location) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red)
                                }
                            }
                            Text(
                                text = "Tarih: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr")).format(Date(location.timestamp))}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${location.lat},${location.lng}&mode=d")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try { context.startActivity(mapIntent) } catch (e: Exception) { }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Arabayla", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${location.lat},${location.lng}&mode=w")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try { context.startActivity(mapIntent) } catch (e: Exception) { }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yürüyerek", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
