package com.example.tossday

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.tossday.data.repository.SettingsRepository
import com.example.tossday.navigation.AppNavGraph
import com.example.tossday.notifications.DailySummaryScheduler
import com.example.tossday.ui.theme.TossDayTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently handle denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        dailySummaryScheduler.scheduleDailySummaries()

        enableEdgeToEdge()
        setContent {
            val appTheme by settingsRepository.appTheme.collectAsState()
            TossDayTheme(appTheme = appTheme) {
                AppNavGraph(navController = rememberNavController())
            }
        }
    }
}
