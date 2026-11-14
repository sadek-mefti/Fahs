package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FahsTheme
import com.example.ui.viewmodel.FahsViewModel
import com.example.ui.viewmodel.NavTab

class MainActivity : ComponentActivity() {
    private val viewModel: FahsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            FahsTheme(
                language = currentLanguage,
                themeMode = themeMode,
                isDarkInstrumentScreen = selectedTab == NavTab.OBD
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
