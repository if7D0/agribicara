package com.agribicara.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agribicara.app.presentation.home.HomeScreen
import com.agribicara.app.presentation.onboarding.OnboardingScreen
import com.agribicara.app.presentation.region.RegionPickerScreen
import com.agribicara.app.presentation.voice.VoiceScreen
import com.agribicara.app.presentation.weather.WeatherScreen

@Composable
fun AppNavigation(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    // Setelah onboarding, wilayah pasti belum dipilih — antar
                    // langsung ke picker, bukan ke Home yang akan kosong.
                    navController.navigate(Route.REGION_PICKER) {
                        launchSingleTop = true
                        // Hapus onboarding dari back stack supaya tombol back
                        // tidak mengembalikan pengguna ke onboarding.
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.REGION_PICKER) {
            RegionPickerScreen(
                onCompleted = {
                    // Picker bisa dicapai dari dua arah: onboarding (Home belum
                    // ada di stack) dan layar cuaca (Home sudah ada, dengan
                    // WEATHER di atasnya). Menavigasi ke HOME begitu saja pada
                    // kasus kedua akan MENUMPUK entri Home kedua di atas
                    // WEATHER, sehingga back dari Home justru masuk ke layar
                    // cuaca. Jadi: kembali ke Home yang sudah ada bila memang
                    // ada, kalau tidak baru buat.
                    val returnedToHome = navController.popBackStack(Route.HOME, false)
                    if (!returnedToHome) {
                        navController.navigate(Route.HOME) {
                            launchSingleTop = true
                            popUpTo(Route.REGION_PICKER) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Route.HOME) {
            HomeScreen(
                onOpenWeather = {
                    navController.navigate(Route.WEATHER) { launchSingleTop = true }
                },
                onChooseRegion = {
                    navController.navigate(Route.REGION_PICKER) { launchSingleTop = true }
                },
                onOpenVoice = {
                    navController.navigate(Route.VOICE) { launchSingleTop = true }
                },
            )
        }

        composable(Route.WEATHER) {
            WeatherScreen(
                onChooseRegion = {
                    navController.navigate(Route.REGION_PICKER) { launchSingleTop = true }
                },
            )
        }

        // Back dari sini kembali ke Home secara alami: VOICE selalu didorong di
        // atas HOME, jadi tidak perlu penanganan back stack khusus seperti pada
        // region picker.
        composable(Route.VOICE) {
            VoiceScreen()
        }
    }
}
