package com.attendance.tracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNavigateToAddEditSubject: (Long) -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        TabItem("Dashboard", Icons.Default.Home, Screen.Dashboard),
        TabItem("Subjects", Icons.Default.List, Screen.Subjects),
        TabItem("Schedule", Icons.Default.DateRange, Screen.Schedule),
        TabItem("Settings", Icons.Default.Settings, Screen.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Subjects.route) {
                SubjectsScreen(
                    onNavigateToAddEditSubject = onNavigateToAddEditSubject
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Future scalability placeholders (Phase 2 features ready to be wired up)
            composable(Screen.Ocr.route) {
                PlaceholderScreen(title = "OCR Screen")
            }
            composable(Screen.Planner.route) {
                PlaceholderScreen(title = "Planner Screen")
            }
            composable(Screen.Semester.route) {
                PlaceholderScreen(title = "Semester Screen")
            }
            composable(Screen.Backup.route) {
                PlaceholderScreen(title = "Backup Screen")
            }
        }
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
