package com.jarvis.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jarvis.android.R
import com.jarvis.android.data.SettingsRepository
import com.jarvis.android.network.JarvisApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSettingsSaved: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val apiClient = remember { JarvisApiClient() }
    val scope = rememberCoroutineScope()

    val savedAuthToken by settingsRepository.authToken.collectAsState(initial = "")
    val savedTelegramId by settingsRepository.telegramId.collectAsState(initial = 0L)
    val savedApiKey by settingsRepository.porcupineApiKey.collectAsState(initial = "")

    var porcupineApiKey by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }

    val isAuthenticated = savedAuthToken.isNotBlank()

    LaunchedEffect(savedApiKey) {
        if (porcupineApiKey.isEmpty() && savedApiKey.isNotEmpty()) {
            porcupineApiKey = savedApiKey
        }
    }

    if (showQrScanner) {
        QrScannerScreen(
            onCodeScanned = { code ->
                showQrScanner = false
                scope.launch {
                    isVerifying = true
                    errorMessage = null
                    successMessage = null

                    // Extract code from QR (might be URL or just code)
                    val qrCode = extractCodeFromQr(code)

                    val result = apiClient.verifyQrCode(qrCode)

                    if (result.success) {
                        settingsRepository.saveAuthData(
                            token = result.token,
                            telegramId = result.telegramId,
                            expiresAt = result.expiresAt
                        )
                        successMessage = "Authenticated successfully!"
                    } else {
                        errorMessage = result.error.ifEmpty { "Verification failed" }
                    }
                    isVerifying = false
                }
            },
            onCancel = { showQrScanner = false }
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Auth Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAuthenticated)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAuthenticated) stringResource(R.string.authenticated) else stringResource(R.string.not_authenticated),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAuthenticated)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (isAuthenticated && savedTelegramId > 0) {
                        Text(
                            text = "Telegram ID: $savedTelegramId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isAuthenticated) {
                // QR Code Scan Button
                Button(
                    onClick = { showQrScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...")
                    } else {
                        Text("Scan QR Code")
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                successMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Logout button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.clearAuth()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.logout))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // Porcupine API Key
            Text(
                text = "Wake Word Settings",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = porcupineApiKey,
                onValueChange = { porcupineApiKey = it },
                label = { Text(stringResource(R.string.porcupine_api_key)) },
                placeholder = { Text(stringResource(R.string.porcupine_api_key_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Get free API key at console.picovoice.ai",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.savePorcupineApiKey(porcupineApiKey)
                        onSettingsSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = porcupineApiKey.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

private fun extractCodeFromQr(qrContent: String): String {
    // QR might contain just the code, or a URL with the code
    // Handle various formats
    return when {
        qrContent.contains("/") -> {
            // It's a URL, extract the last part
            qrContent.substringAfterLast("/").substringBefore("?")
        }
        else -> qrContent.trim().uppercase()
    }
}
