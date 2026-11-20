package com.sara.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sara.assistant.ui.screens.HomeScreen
import com.sara.assistant.ui.theme.SaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaraTheme {
                HomeScreen()
            }
        }
    }
}
