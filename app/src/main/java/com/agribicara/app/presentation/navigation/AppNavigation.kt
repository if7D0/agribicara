package com.agribicara.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agribicara.app.presentation.home.HomeScreen
import com.agribicara.app.presentation.onboarding.OnboardingScreen

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
                    navController.navigate(Route.HOME) {
                        launchSingleTop = true
                        // Hapus onboarding dari back stack supaya tombol back
                        // di Home keluar dari app, bukan kembali ke onboarding.
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.HOME) {
            HomeScreen()
        }
    }
}
