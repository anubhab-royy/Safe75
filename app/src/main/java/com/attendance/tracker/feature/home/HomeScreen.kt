package com.attendance.tracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.attendance.tracker.core.navigation.Screen
import com.attendance.tracker.feature.dashboard.DashboardScreen
import com.attendance.tracker.feature.schedule.ScheduleScreen
import com.attendance.tracker.feature.settings.SettingsScreen
import com.attendance.tracker.feature.subject.SubjectsScreen

/**
 * HomeScreen is the bottom navigation shell post-splash/welcome, managing sub-destinations
 * via a nested child NavHost for enhanced modularity.
 */
@Composable
fun HomeScreen(
    initialTabRoute: String = Screen.Dashboard.route,
    onNavigateToAddEditSubject: (Long) -> Unit,
    onNavigateToAddEditSchedule: (Long) -> Unit,
    onNavigateToAttendanceHistory: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToLeavePlanner: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToRestore: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToArchiveDetails: (Long) -> Unit,
    onNavigateToSemesterReset: () -> Unit,
    onNavigateToIntegrity: () -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        TabItem("Dashboard", Icons.Default.Home, Screen.Dashboard),
        TabItem("Subjects", Icons.Default.List, Screen.Subjects),
        TabItem("Schedule", Icons.Default.DateRange, Screen.Schedule),
        TabItem("Settings", Icons.Default.Settings, Screen.Settings)
    )

    // Wide screens (tablets / large landscape) use a NavigationRail instead of a
    // bottom NavigationBar for a more adaptive, desktop-like layout.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = isSelected,
                                onClick = { navController.selectTab(item) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            NavigationRailItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = isSelected,
                                onClick = { navController.selectTab(item) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = initialTabRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onNavigateToAttendanceHistory = onNavigateToAttendanceHistory,
                            onNavigateToSimulator = onNavigateToSimulator,
                            onNavigateToLeavePlanner = onNavigateToLeavePlanner
                        )
                    }
                    composable(Screen.Subjects.route) {
                        SubjectsScreen(
                            onNavigateToAddEditSubject = onNavigateToAddEditSubject
                        )
                    }
                    composable(Screen.Schedule.route) {
                        ScheduleScreen(
                            onNavigateToAddEditSchedule = onNavigateToAddEditSchedule
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
                            onNavigateToOcr = onNavigateToOcr,
                            onNavigateToBackup = onNavigateToBackup,
                            onNavigateToRestore = onNavigateToRestore,
                            onNavigateToArchive = onNavigateToArchive,
                            onNavigateToSemesterReset = onNavigateToSemesterReset,
                            onNavigateToIntegrity = onNavigateToIntegrity
                        )
                    }

                    // Future module placeholder (Phase 2 feature ready to be wired up)
                    composable(Screen.Planner.route) {
                        PlaceholderScreen(title = "Planner Screen")
                    }
                }
            }
        }
    }
}

private fun androidx.navigation.NavHostController.selectTab(item: TabItem) {
    navigate(item.screen.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: Screen
)
