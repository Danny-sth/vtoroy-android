package com.vtoroy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.vtoroy.android.data.SettingsRepository
import com.vtoroy.android.ui.VtoroyApp
import com.vtoroy.android.ui.theme.VtoroyAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow configuration via Intent extras (for adb setup)
        handleConfigIntent()

        enableEdgeToEdge()
        setContent {
            VtoroyAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VtoroyApp()
                }
            }
        }
    }

    private fun handleConfigIntent() {
        val authToken = intent.getStringExtra("auth_token")
        val porcupineKey = intent.getStringExtra("porcupine_key")

        if (authToken != null || porcupineKey != null) {
            lifecycleScope.launch {
                authToken?.let { settingsRepository.saveAuthToken(it) }
                porcupineKey?.let { settingsRepository.savePorcupineApiKey(it) }
            }
        }
    }
}
