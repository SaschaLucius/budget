package com.lukulent.finanzapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.lukulent.finanzapp.navigation.AppNavigation
import com.lukulent.finanzapp.settings.SettingsDataStore
import com.lukulent.finanzapp.update.ReleaseInfo
import com.lukulent.finanzapp.update.fetchLatestRelease
import com.lukulent.finanzapp.update.isNewerVersion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as FinanzApp
        @Suppress("DEPRECATION")
        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
        setContent {
            val bgColorLong by app.settingsDataStore.backgroundColor.collectAsState(
                initial = SettingsDataStore.DEFAULT_BACKGROUND_COLOR
            )
            val bgColor = Color(bgColorLong)
            var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

            val isLight = bgColor.luminance() > 0.5f
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }

            LaunchedEffect(Unit) {
                val release = fetchLatestRelease()
                if (release != null && isNewerVersion(release.tagName, currentVersion)) {
                    updateInfo = release
                }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = bgColor,
                    surface = bgColor
                )
            ) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)

                updateInfo?.let { release ->
                    AlertDialog(
                        onDismissRequest = { updateInfo = null },
                        title = { Text("Update verfügbar") },
                        text = { Text("Version ${release.tagName} ist verfügbar. Jetzt herunterladen?") },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)))
                                updateInfo = null
                            }) {
                                Text("Herunterladen")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateInfo = null }) {
                                Text("Später")
                            }
                        }
                    )
                }
            }
        }
    }
}
