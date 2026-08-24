package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.EmbossedCard
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

@Composable
fun ProfileSetupScreen(onProfileSaved: (String, String, String?) -> Unit) {
    var nick by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = com.example.util.FileHelper.copyUriToInternalStorage(context, it, "profile_avatar_${System.currentTimeMillis()}.jpg")
            if (localPath != null) {
                selectedAvatarUri = localPath
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F2ED)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header 3D Section
            EmbossedCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevation = 6.dp,
                contentPadding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profilini Oluştur",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "HatırlaGit kişisel asistanını kişiselleştir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // Avatar Selection Circle
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                Brush.sweepGradient(listOf(OrangePrimary, TurquoiseSecondary, OrangePrimary)),
                                CircleShape
                            )
                            .background(Color.White)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Profil Resmi",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = TurquoiseSecondary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Fotoğraf Seç",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fotoğraf yüklemek için dokunun (İsteğe bağlı)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Nickname Input
                    OutlinedTextField(
                        value = nick,
                        onValueChange = { nick = it },
                        label = { Text("Kullanıcı Adı / Nick", fontWeight = FontWeight.Bold, color = Slate900) },
                        placeholder = { Text("Örn: Deniz, Kaptan, Asistanım...", color = Slate700) },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = OrangePrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // PIN Code Input
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Güvenlik PIN Kodu (4-6 hane)", fontWeight = FontWeight.Bold, color = Slate900) },
                        placeholder = { Text("••••", color = Slate700) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TurquoiseSecondary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoiseSecondary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save Button
            Button(
                onClick = {
                    if (nick.isNotBlank() && pin.isNotBlank()) {
                        onProfileSaved(nick.trim(), pin.trim(), selectedAvatarUri)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                enabled = nick.isNotBlank() && pin.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    "Profili Kaydet ve Başla",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}
