package com.attendance.tracker.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.tracker.feature.home.HomeScreen
import com.attendance.tracker.feature.splash.SplashScreen
import com.attendance.tracker.feature.subject.AddEditSubjectScreen
import com.attendance.tracker.feature.schedule.AddEditScheduleScreen
import com.attendance.tracker.feature.attendance.AttendanceHistoryScreen
import com.attendance.tracker.feature.attendance.AttendanceDetailsScreen
import com.attendance.tracker.feature.planner.AttendanceSimulatorScreen
import com.attendance.tracker.feature.planner.LeavePlannerScreen
import com.attendance.tracker.feature.settings.NotificationSettingsScreen
import com.attendance.tracker.feature.ocr.OCRImportScreen
import com.attendance.tracker.feature.ocr.OCRReviewScreen
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
            HomeScreen(
                onNavigateToAddEditSubject = { subjectId ->
                    navController.navigate(Screen.AddEditSubject.createRoute(subjectId))
                },
                onNavigateToAddEditSchedule = { scheduleId ->
                    navController.navigate(Screen.AddEditSchedule.createRoute(scheduleId))
                },
                onNavigateToAttendanceHistory = {
                    navController.navigate(Screen.AttendanceHistory.route)
                },
                onNavigateToSimulator = {
                    navController.navigate(Screen.AttendanceSimulator.route)
                },
                onNavigateToLeavePlanner = {
                    navController.navigate(Screen.LeavePlanner.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToOcr = {
                    navController.navigate(Screen.Ocr.route)
                }
            )
        }

        // Detail Screens hosted outside the bottom bar stack
        composable(
            route = Screen.AddEditSubject.route,
            arguments = listOf(
                navArgument("subjectId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: -1L
            AddEditSubjectScreen(
                subjectId = subjectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AddEditSchedule.route,
            arguments = listOf(
                navArgument("scheduleId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: -1L
            AddEditScheduleScreen(
                scheduleId = scheduleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AttendanceHistory.route) {
            AttendanceHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAttendanceDetails = { attendanceId ->
                    navController.navigate(Screen.AttendanceDetails.createRoute(attendanceId))
                }
            )
        }

        composable(
            route = Screen.AttendanceDetails.route,
            arguments = listOf(
                navArgument("attendanceId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val attendanceId = backStackEntry.arguments?.getLong("attendanceId") ?: 0L
            AttendanceDetailsScreen(
                attendanceId = attendanceId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AttendanceSimulator.route) {
            AttendanceSimulatorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LeavePlanner.route) {
            LeavePlannerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Ocr.route) {
            OCRImportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToReview = { isTimetable, imageUri ->
                    navController.navigate("ocr_review?isTimetable=$isTimetable&uri=${Uri.encode(imageUri.toString())}")
                }
            )
        }

        composable(
            route = "ocr_review?isTimetable={isTimetable}&uri={uri}",
            arguments = listOf(
                navArgument("isTimetable") { type = NavType.BoolType },
                navArgument("uri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val isTimetable = backStackEntry.arguments?.getBoolean("isTimetable") ?: true
            val uriStr = backStackEntry.arguments?.getString("uri") ?: ""
            OCRReviewScreen(
                isTimetable = isTimetable,
                imageUri = Uri.parse(uriStr),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
