package com.lukulent.finanzapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.lukulent.finanzapp.navigation.AppNavigation
import com.lukulent.finanzapp.settings.SettingsDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as FinanzApp
        setContent {
            val bgColorLong by app.settingsDataStore.backgroundColor.collectAsState(
                initial = SettingsDataStore.DEFAULT_BACKGROUND_COLOR
            )
            val bgColor = Color(bgColorLong)
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = bgColor,
                    surface = bgColor
                )
            ) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
