package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.ApiResult
import com.example.core.network.AuthSession
import com.example.domain.usecase.AuthUseCase
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authUseCase: AuthUseCase,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(color = CarbonDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonCard)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isRegisterMode) "CREATE RACER ID" else "PILOT SIGN IN",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
            }
        },
        containerColor = CarbonDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .testTag("auth_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isRegisterMode) "Register your racer credentials to save career progress in the cloud." else "Enter your credentials to sync with the global leaderboard.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Racer Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CarbonCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonOrange) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonOrange,
                    unfocusedBorderColor = CarbonCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = RacingRed)
                )
            }

            if (successMessage != null) {
                Text(
                    text = successMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldGreen)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NeonButton(
                text = if (isLoading) "CONNECTING..." else if (isRegisterMode) "REGISTER RACER" else "SIGN IN",
                icon = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.Login,
                color = NeonCyan,
                enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = if (isRegisterMode) {
                            authUseCase.registerEmail(email.trim(), password)
                        } else {
                            authUseCase.loginEmail(email.trim(), password)
                        }
                        isLoading = false
                        when (result) {
                            is ApiResult.Success -> {
                                successMessage = "Connected successfully!"
                                onAuthSuccess()
                            }
                            is ApiResult.Error -> {
                                errorMessage = result.message
                            }
                            ApiResult.NetworkUnavailable -> {
                                errorMessage = "Network unavailable. Operating in local mode."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isRegisterMode) "Already have a profile? Sign In" else "New racer? Register Account",
                    color = TextMuted
                )
            }
        }
    }
}
