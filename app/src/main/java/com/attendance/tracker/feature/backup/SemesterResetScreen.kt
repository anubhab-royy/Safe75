package com.attendance.tracker.feature.backup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions

/**
 * Multi-step semester reset wizard.
 *
 * Step 0: Choose what to keep / delete.
 * Step 1: Preview affected record counts (loaded live from the database).
 * Step 2: Confirm via [ResetConfirmationDialog] by typing "RESET".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterResetScreen(
    onNavigateBack: () -> Unit,
    viewModel: SemesterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.resetResult) {
        when (val r = state.resetResult) {
            is ResetResult.Success -> {
                snackbarHostState.showSnackbar(
                    "Reset complete: ${r.attendanceDeleted} attendance records removed."
                )
                viewModel.clearResult()
                onNavigateBack()
            }
            is ResetResult.Failure -> {
                snackbarHostState.showSnackbar("Reset failed: ${r.error}")
                viewModel.clearResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Semester Reset",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(Dimensions.SpacingMedium)
        ) {
            // Step indicator
            StepIndicator(currentStep = state.currentStep)
            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "reset_wizard"
            ) { step ->
                when (step) {
                    0 -> ResetOptionsStep(
                        options = state.options,
                        onOptionsChanged = viewModel::updateOptions,
                        onNext = { viewModel.goToStep(1) }
                    )
                    1 -> ResetPreviewStep(
                        options = state.options,
                        preview = state.resetPreview,
                        onBack = { viewModel.goToStep(0) },
                        onNext = { viewModel.goToStep(2) }
                    )
                    else -> Spacer(Modifier.height(0.dp))
                }
            }
        }
    }

    // Final confirmation dialog (step 2).
    if (state.currentStep == 2) {
        ResetConfirmationDialog(
            confirmationText = state.confirmationText,
            onTextChange = viewModel::updateConfirmationText,
            isLoading = state.isLoading,
            onConfirm = viewModel::executeReset,
            onDismiss = { viewModel.goToStep(1) }
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    val steps = listOf("Options", "Preview", "Confirm")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (index <= currentStep) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (index <= currentStep) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ResetOptionsStep(
    options: ResetOptions,
    onOptionsChanged: (ResetOptions) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Choose what to keep:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        ResetToggleRow("Keep Subjects", "Your subject list will not be deleted", options.keepSubjects, Icons.Default.Book) {
            onOptionsChanged(options.copy(keepSubjects = it))
        }
        ResetToggleRow("Keep Schedules", "Timetable entries will not be deleted", options.keepSchedules, Icons.Default.DateRange) {
            onOptionsChanged(options.copy(keepSchedules = it))
        }
        ResetToggleRow("Keep Settings", "Theme and notification settings preserved", options.keepSettings, Icons.Default.Settings) {
            onOptionsChanged(options.copy(keepSettings = it))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Choose what to delete:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        ResetToggleRow("Delete Attendance Records", "All attendance history will be removed", options.deleteAttendance, Icons.Default.Delete, tintRed = true) {
            onOptionsChanged(options.copy(deleteAttendance = it))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Preview Impact")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ResetToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    tintRed: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tintRed && checked)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = if (tintRed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ResetPreviewStep(
    options: ResetOptions,
    preview: ResetPreview?,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reset Impact Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewRow("Subjects", if (options.keepSubjects) "Preserved ✓" else "Will be deleted ✗", !options.keepSubjects)
                PreviewRow("Schedules", if (options.keepSchedules) "Preserved ✓" else "Will be deleted ✗", !options.keepSchedules)
                PreviewRow("Attendance", if (options.deleteAttendance) "Will be deleted ✗" else "Preserved ✓", options.deleteAttendance)
                PreviewRow("Settings", if (options.keepSettings) "Preserved ✓" else "Will be deleted ✗", !options.keepSettings)
            }
        }

        preview?.let { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Affected records currently in the database:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    PreviewRow("Subjects", "${p.subjectCount}", false)
                    PreviewRow("Schedules", "${p.scheduleCount}", false)
                    PreviewRow("Attendance records", "${p.attendanceCount}", options.deleteAttendance)
                    PreviewRow("Semester versions", "${p.semesterVersionCount}", false)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    "This action cannot be undone. Create a backup first if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Continue") }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, isDestructive: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}
