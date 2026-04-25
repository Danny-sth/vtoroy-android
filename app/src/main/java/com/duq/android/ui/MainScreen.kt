package com.duq.android.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.duq.android.DuqState
import com.duq.android.error.DuqError
import com.duq.android.service.DuqListenerService
import com.duq.android.service.VoiceServiceController
import com.duq.android.ui.components.ArcReactor
import com.duq.android.ui.components.MessagesList
import com.duq.android.ui.theme.DuqColors

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var voiceController by remember { mutableStateOf<VoiceServiceController?>(null) }

    val state by voiceController?.state?.collectAsState() ?: remember { mutableStateOf(DuqState.IDLE) }
    val serviceError by voiceController?.error?.collectAsState() ?: remember { mutableStateOf<DuqError?>(null) }
    val viewModelError by viewModel.error.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Combine errors - service error takes priority
    val currentError = serviceError ?: viewModelError

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                voiceController = (binder as? DuqListenerService.LocalBinder)?.getController()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                voiceController = null
            }
        }
    }

    // Track lifecycle state
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBound by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Just mark permissions as granted - binding happens in lifecycle observer
        permissionsGranted = permissions.values.all { it }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Start foreground service and bind when permissions are granted
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !isBound) {
            // Start as foreground service for background operation
            val serviceIntent = Intent(context, DuqListenerService::class.java).apply {
                action = DuqListenerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            // Also bind to get service reference for UI updates
            context.bindService(
                Intent(context, DuqListenerService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            isBound = true
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Rebind to get service reference for UI updates
                    if (permissionsGranted && !isBound) {
                        context.bindService(
                            Intent(context, DuqListenerService::class.java),
                            serviceConnection,
                            Context.BIND_AUTO_CREATE
                        )
                        isBound = true
                    }
                    // Always refresh messages when app gains focus
                    viewModel.refreshMessages()
                }
                Lifecycle.Event.ON_STOP -> {
                    // Only unbind, don't stop service - it should keep running for WebSocket
                    if (isBound) {
                        try { context.unbindService(serviceConnection) } catch (_: Exception) {}
                        isBound = false
                        voiceController = null
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isBound) {
                try { context.unbindService(serviceConnection) } catch (_: Exception) {}
                // Don't stop service - let it manage its own lifecycle
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DuqColors.background)
    ) {
        // Settings button - top right
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(DuqColors.surfaceVariant)
                .clickable { onNavigateToSettings() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u2699",
                fontSize = 22.sp,
                color = DuqColors.textSecondary
            )
        }

        // Main content - Arc Reactor + Messages
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp) // Space for settings button
        ) {
            // Arc Reactor + Status - top section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArcReactor(
                    state = state,
                    modifier = Modifier.size(180.dp) // Reduced from 240dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = getStatusText(state, currentError),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = if (currentError != null) DuqColors.error else DuqColors.textSecondary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
            }

            // Messages list - bottom section
            MessagesList(
                messages = messages,
                isLoading = isLoading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun getStatusText(state: DuqState, error: DuqError?): String {
    // Show error message if in error state and error exists
    if (state == DuqState.ERROR && error != null) {
        return error.toDisplayMessage().uppercase()
    }
    return when (state) {
        DuqState.IDLE -> "SAY \"HEY DUQ\""
        DuqState.LISTENING, DuqState.RECORDING -> "LISTENING..."
        DuqState.PROCESSING -> "PROCESSING..."
        DuqState.PLAYING -> "SPEAKING..."
        DuqState.ERROR -> "ERROR"
    }
}

