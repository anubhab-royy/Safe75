package com.attendance.tracker.feature.settings

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions

/**
 * Screen presenting general settings options, including theme toggles,
 * notifications, data management (backup / archive / reset), and OCR import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToRestore: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToSemesterReset: () -> Unit,
    onNavigateToIntegrity: () -> Unit,
    onNavigateToBugReport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val activeTheme by viewModel.themeState.collectAsStateWithLifecycle()
    val currentGoal by viewModel.attendanceGoal.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                showBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(Dimensions.SpacingMedium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            // Notification settings link card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNotificationSettings() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Dimensions.SpacingMedium)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Configure morning schedule, post-class logging actions, and night reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // OCR Import settings link card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToOcr() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Dimensions.SpacingMedium)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OCR Timetable / Attendance Import",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Import schedule version timetables or ERP attendance statements via photo scanning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Attendance Goal settings card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGoalDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Dimensions.SpacingMedium)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance Goal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val goalText = if (currentGoal in listOf(75.0, 80.0, 85.0, 90.0, 95.0)) {
                            "${currentGoal.toInt()}%"
                        } else {
                            "Custom (${currentGoal.toInt()}%)"
                        }
                        Text(
                            text = "Current Goal: $goalText. Tap to configure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (showGoalDialog) {
                AttendanceGoalDialog(
                    currentGoal = currentGoal,
                    onDismiss = { showGoalDialog = false },
                    onConfirm = { newGoal ->
                        viewModel.setAttendanceGoal(newGoal)
                    }
                )
            }

            // Data Management section (Phase 7)
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            SettingsLinkCard(
                icon = Icons.Default.Backup,
                title = "Backup & Restore",
                subtitle = "Export your data as a JSON backup file.",
                onClick = onNavigateToBackup
            )
            SettingsLinkCard(
                icon = Icons.Default.Restore,
                title = "Restore from Backup",
                subtitle = "Import and selectively restore a backup file.",
                onClick = onNavigateToRestore
            )
            SettingsLinkCard(
                icon = Icons.Default.Archive,
                title = "Semester Archive",
                subtitle = "Browse archived semesters and restore past data.",
                onClick = onNavigateToArchive
            )
            SettingsLinkCard(
                icon = Icons.Default.RestartAlt,
                title = "Semester Reset",
                subtitle = "Archive the current semester and start fresh.",
                onClick = onNavigateToSemesterReset
            )
            SettingsLinkCard(
                icon = Icons.Default.HealthAndSafety,
                title = "Data Integrity",
                subtitle = "Scan the database for broken references and orphan records.",
                onClick = onNavigateToIntegrity
            )
            SettingsLinkCard(
                icon = Icons.Default.Info,
                title = "Report a Problem",
                subtitle = "Send a description and optional screenshot to Safe75 support.",
                onClick = onNavigateToBugReport
            )

            // Theme Options Panel
            Text(
                text = "Application Theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeOptionRow(
                    label = "System Default",
                    isSelected = activeTheme == "SYSTEM",
                    onClick = { viewModel.setThemeMode("SYSTEM") }
                )
                ThemeOptionRow(
                    label = "Light Mode",
                    isSelected = activeTheme == "LIGHT",
                    onClick = { viewModel.setThemeMode("LIGHT") }
                )
                ThemeOptionRow(
                    label = "Dark Mode",
                    isSelected = activeTheme == "DARK",
                    onClick = { viewModel.setThemeMode("DARK") }
                )
            }
        }
    }
}

@Composable
private fun SettingsLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Dimensions.SpacingMedium)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AttendanceGoalDialog(
    currentGoal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val options = listOf(75.0, 80.0, 85.0, 90.0, 95.0)
    var selectedOption by remember {
        mutableStateOf(
            if (options.contains(currentGoal)) currentGoal else -1.0
        )
    }
    var customInput by remember {
        mutableStateOf(
            if (selectedOption == -1.0) currentGoal.toInt().toString() else ""
        )
    }
    var isInputError by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_attendance_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedOption = opt
                                isInputError = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == opt,
                            onClick = {
                                selectedOption = opt
                                isInputError = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.str_res_1, opt.toInt()))
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = -1.0 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == -1.0,
                        onClick = { selectedOption = -1.0 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.custom))
                }

                if (selectedOption == -1.0) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            val value = it.toIntOrNull()
                            isInputError = value == null || value !in 50..100
                        },
                        label = { Text(stringResource(R.string.custom_goal)) },
                        isError = isInputError,
                        supportingText = {
                            if (isInputError) {
                                Text(stringResource(R.string.enter_a_percentage_between_50))
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val finalGoal = if (selectedOption != -1.0) {
                        selectedOption
                    } else {
                        customInput.toDoubleOrNull() ?: 75.0
                    }
                    onConfirm(finalGoal)
                    onDismiss()
                },
                enabled = selectedOption != -1.0 || (!isInputError && customInput.isNotBlank())
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
