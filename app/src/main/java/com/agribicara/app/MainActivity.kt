package com.agribicara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.agribicara.app.presentation.navigation.AppNavigation
import com.agribicara.app.presentation.theme.AgriBicaraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 37 memaksa edge-to-edge dan membuat status bar transparan.
        // Tanpa gaya eksplisit ini, ikon sistem tetap terang di atas latar terang
        // aplikasi sehingga jam/baterai/sinyal praktis tak terlihat — bertentangan
        // langsung dengan syarat kontras tinggi untuk pemakaian di luar ruangan.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AgriBicaraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val startState by viewModel.startState.collectAsStateWithLifecycle()

                    when (val state = startState) {
                        // Layar kosong sesaat: mencegah kedipan Onboarding -> Home.
                        StartState.Loading -> Unit
                        is StartState.Ready -> AppNavigation(
                            startDestination = state.route,
                            navController = rememberNavController(),
                        )
                    }
                }
            }
        }
    }
}
