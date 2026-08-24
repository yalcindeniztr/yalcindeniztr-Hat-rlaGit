package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary

/**
 * Dialog allowing the user to set, change, or remove a PIN lock for a specific category.
 * Highlights AES-256 local encrypted storage.
 */
@Composable
fun CategoryLockDialog(
    categoryName: String,
    isCurrentlyLocked: Boolean,
    onDismiss: () -> Unit,
    onSaveLock: (pin: String, isLocked: Boolean) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF7E22CE), Color(0xFF3B82F6)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isCurrentlyLocked) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$categoryName Şifreleme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Security Reassurance Card
                EmbossedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 10.dp,
                    elevation = 2.dp,
                    glowColor = Color(0xFFC084FC),
                    contentPadding = 10.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🛡️ %100 Cihaz İçi Şifreleme: PIN şifreniz Android KeyStore (AES-256) donanımında saklanır, asla internete veya sunuculara iletilmez.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF581C87),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isCurrentlyLocked) {
                    Text(
                        text = "Bu kategori şu anda kilitli. Kilidi kaldırmak veya yeni bir PIN belirlemek için seçim yapabilirsiniz.",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = if (isCurrentlyLocked) "Yeni PIN Girin (Kilidi değiştirmek için)" else "4-6 Haneli Kategori PIN Şifresi Belirleyin",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN Şifresi (Örn: 1234)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7E22CE),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN Şifresini Tekrar Edin") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7E22CE),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = "Lütfen en az 4 haneli bir PIN girin."
                    } else if (pin != confirmPin) {
                        errorMessage = "Girdiğiniz PIN şifreleri birbiriyle eşleşmiyor."
                    } else {
                        onSaveLock(pin, true)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isCurrentlyLocked) "PIN'i Güncelle" else "Kategoriyi Kilitle")
            }
        },
        dismissButton = {
            Row {
                if (isCurrentlyLocked) {
                    TextButton(
                        onClick = {
                            onSaveLock("", false)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kilidi Kaldır", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        }
    )
}

/**
 * Dialog prompting the user for PIN to access a locked category.
 */
@Composable
fun CategoryUnlockDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onVerifyPin: (enteredPin: String) -> Unit,
    isError: Boolean = false
) {
    var enteredPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7E22CE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Korumalı Kategori",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "'$categoryName' kategorisine erişmek için lütfen belirlediğiniz PIN şifresini girin.",
                    fontSize = 13.sp,
                    color = Slate800
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            enteredPin = it
                        }
                    },
                    label = { Text("Kategori PIN Şifresi") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7E22CE),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hatalı PIN şifresi! Lütfen tekrar deneyin.",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🔒 Özel verileriniz telefonunuzda güvenle korunmaktadır.",
                    fontSize = 11.sp,
                    color = Slate700
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifyPin(enteredPin) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Giriş Yap")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Slate700)
            }
        }
    )
}
