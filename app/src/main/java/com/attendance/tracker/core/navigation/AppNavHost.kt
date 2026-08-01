package com.attendance.tracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.attendance.tracker.feature.home.HomeScreen
import com.attendance.tracker.feature.splash.SplashScreen
import com.attendance.tracker.feature.welcome.WelcomeScreen

/**
 * Root Navigation Graph configuring Splash, Welcome, and transitions to Main content.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RootGraph.route,
        modifier = modifier
    ) {
        // Root Graph: Handles splash/onboarding
        navigation(
            startDestination = Screen.Splash.route,
            route = Screen.RootGraph.route
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateNext = {
                        navController.navigate(Screen.MainGraph.route) {
                            popUpTo(Screen.RootGraph.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Main Graph: Launches bottom navigation shell
        composable(route = Screen.MainGraph.route) {
            HomeScreen()
        }
    }
}
