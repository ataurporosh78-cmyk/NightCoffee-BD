package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.FirebaseSafeAccess
import com.example.ui.NightCoffeeNavigation
import com.example.ui.theme.NightCoffeeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
        }
        FirebaseSafeAccess.ensureInitialized(applicationContext)
        setContent {
            NightCoffeeTheme {
                NightCoffeeNavigation()
            }
        }
    }
}
