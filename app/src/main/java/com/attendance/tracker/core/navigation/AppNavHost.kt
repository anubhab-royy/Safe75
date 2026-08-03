package com.attendance.tracker.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.tracker.core.widget.WidgetIntent
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
import com.attendance.tracker.feature.semester.SemesterSetupScreen
import com.attendance.tracker.feature.archive.ArchiveDetailsScreen
import com.attendance.tracker.feature.archive.ArchiveScreen
import com.attendance.tracker.feature.backup.BackupScreen
import com.attendance.tracker.feature.backup.RestoreScreen
import com.attendance.tracker.feature.backup.SemesterResetScreen
import com.attendance.tracker.feature.backfill.BackfillWizardScreen
import com.attendance.tracker.feature.integrity.IntegrityScreen

/**
 * Root Navigation Graph configuring Splash, Welcome, and transitions to Main content.
 *
 * @param startDestination Optional widget destination token (see
 *   [WidgetIntent]). When provided the app skips the splash/onboarding flow and
 *   launches directly into the requested screen (dashboard, attendance history,
 *   schedule, or settings).
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startDestination: String? = null,
    navController: NavHostController = rememberNavController()
) {
    val opensMainGraph = WidgetIntent.opensMainGraph(startDestination)
    val initialRoute = if (opensMainGraph) Screen.MainGraph.route else Screen.RootGraph.route

    NavHost(
        navController = navController,
        startDestination = initialRoute,
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
                        navController.navigate(Screen.SemesterSetup.createRoute(-1L)) {
                            popUpTo(Screen.RootGraph.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Main Graph: Launches bottom navigation shell
        composable(route = Screen.MainGraph.route) {
            HomeScreen(
                initialTabRoute = WidgetIntent.routeFor(startDestination),
                onNavigateToAddEditSubject = { subjectId ->
                    navController.navigate(Screen.AddEditSubject.createRoute(subjectId))
                },
                onNavigateToAddEditSchedule = { scheduleId ->
                    navController.navigate(Screen.AddEditSchedule.createRoute(scheduleId))
                },
                onNavigateToBackfill = { subjectId ->
                    navController.navigate(Screen.BackfillWizard.createRoute(subjectId))
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
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToRestore = {
                    navController.navigate(Screen.Restore.route)
                },
                onNavigateToArchive = {
                    navController.navigate(Screen.Archive.route)
                },
                onNavigateToArchiveDetails = { archiveId ->
                    navController.navigate(Screen.ArchiveDetails.createRoute(archiveId))
                },
                onNavigateToSemesterReset = {
                    navController.navigate(Screen.SemesterReset.route)
                },
                onNavigateToIntegrity = {
                    navController.navigate(Screen.Integrity.route)
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
                },
                onNavigateToBackfill = { subjectId ->
                    navController.navigate(Screen.BackfillWizard.createRoute(subjectId)) {
                        popUpTo(Screen.MainGraph.route)
                    }
                }
            )
        }

        composable(
            route = Screen.BackfillWizard.route,
            arguments = listOf(
                navArgument("subjectId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: -1L
            BackfillWizardScreen(
                subjectId = subjectId,
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

        composable(
            route = Screen.SemesterSetup.route,
            arguments = listOf(
                navArgument("semesterId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            SemesterSetupScreen(
                onNavigateToOcr = {
                    navController.navigate(Screen.Ocr.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.MainGraph.route) {
                        popUpTo(Screen.SemesterSetup.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ---- Data Management & Semester Lifecycle (Phase 7) ----

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRestore = { navController.navigate(Screen.Restore.route) },
                onNavigateToIntegrity = { navController.navigate(Screen.Integrity.route) },
                onNavigateToSemesterReset = { navController.navigate(Screen.SemesterReset.route) }
            )
        }

        composable(Screen.Restore.route) {
            RestoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SemesterReset.route) {
            SemesterResetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Archive.route) {
            ArchiveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { archiveId ->
                    navController.navigate(Screen.ArchiveDetails.createRoute(archiveId))
                }
            )
        }

        composable(
            route = Screen.ArchiveDetails.route,
            arguments = listOf(
                navArgument("archiveId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val archiveId = backStackEntry.arguments?.getLong("archiveId") ?: 0L
            ArchiveDetailsScreen(
                archiveId = archiveId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Integrity.route) {
            IntegrityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Direct-launch from the widget to a detail screen (e.g. attendance history).
    val directDestination = startDestination?.takeIf { !opensMainGraph }?.let { WidgetIntent.routeFor(it) }
    LaunchedEffect(directDestination) {
        if (directDestination != null) {
            navController.navigate(directDestination) {
                launchSingleTop = true
            }
        }
    }
}
