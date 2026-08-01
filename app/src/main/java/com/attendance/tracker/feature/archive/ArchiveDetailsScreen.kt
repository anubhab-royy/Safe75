package com.attendance.tracker.feature.archive

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.ArchiveData
import com.attendance.tracker.domain.model.ArchiveSubjectStat
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.feature.backup.RestoreOptionsDialog
import java.util.Locale

/**
 * Read-only details of an archived semester.
 *
 * Shows the attendance summary and per-subject statistics stored in the
 * archive snapshot. The user can restore the archive (with a warning about
 * overwriting current data) or go back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailsScreen(
    archiveId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(archiveId) {
        viewModel.loadArchiveDetails(archiveId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.restoreResult) {
        when (val r = state.restoreResult) {
            is RestoreResult.Success -> {
                snackbarHostState.showSnackbar(
                    "Restored ${r.subjectsRestored} subjects, ${r.attendanceRestored} attendance records."
                )
                viewModel.clearRestoreResult()
            }
            is RestoreResult.Failure -> {
                snackbarHostState.showSnackbar("Restore failed: ${r.error}")
                viewModel.clearRestoreResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Archive Details",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val archive = state.selectedArchive
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (state.isLoading && archive == null) {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    message = "Loading archive..."
                )
            } else if (archive != null) {
                ArchiveDetailsContent(
                    archive = archive,
                    onRestore = viewModel::showRestoreOptions
                )
            } else {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    message = "Archive not found",
                    description = "It may have been deleted."
                )
            }
        }
    }

    if (state.showRestoreOptions && state.selectedArchive != null) {
        val archive = state.selectedArchive!!
        RestoreOptionsDialog(
            options = state.restoreOptions,
            onOptionsChanged = viewModel::updateRestoreOptions,
            onConfirm = { viewModel.restoreArchive(archive.id) },
            onDismiss = viewModel::dismissRestoreOptions,
            subjectCount = archive.subjectStats.size,
            scheduleCount = archive.totalClasses,
            attendanceCount = archive.totalClasses,
            showSemesterVersions = false,
            showSettings = false,
            showPreviewSummary = true,
            previewTitle = "Archive Preview"
        )
    }
}

@Composable
private fun ArchiveDetailsContent(
    archive: ArchiveData,
    onRestore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimensions.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                    Text(archive.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${archive.startDate} → ${archive.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SummaryStatRow(archive)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Overall attendance: ${String.format(Locale.ROOT, "%.1f%%", archive.overallPercentage)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = attendanceColor(archive.overallPercentage)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Dimensions.SpacingSmall))
                    Column(Modifier.weight(1f)) {
                        Text("Restore this semester", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Restoring overwrites existing subjects, schedules, and attendance with this snapshot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SpacingMedium)
                        .padding(bottom = Dimensions.SpacingMedium)
                ) { Text("Restore Archived Semester") }
            }
        }

        item {
            Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (archive.subjectStats.isEmpty()) {
            item {
                Text(
                    "No subjects recorded in this archive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(archive.subjectStats, key = { it.subjectId }) { stat ->
                SubjectStatCard(stat)
            }
        }
    }
}

@Composable
private fun SummaryStatRow(archive: ArchiveData) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SummaryChip(archive.totalClasses, "Classes")
        SummaryChip(archive.presentCount, "Present")
        SummaryChip(archive.absentCount, "Absent")
        SummaryChip(archive.cancelledCount, "Cancelled")
    }
}

@Composable
private fun SummaryChip(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectStatCard(stat: ArchiveSubjectStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stat.subjectName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${stat.totalClasses} classes • ${stat.presentCount} P • ${stat.absentCount} A • ${stat.cancelledCount} C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                String.format(Locale.ROOT, "%.1f%%", stat.attendancePercentage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = attendanceColor(stat.attendancePercentage)
            )
        }
    }
}

private fun attendanceColor(percentage: Double): Color = when {
    percentage >= 75.0 -> Color(0xFF2E7D32)
    percentage >= 60.0 -> Color(0xFFF9A825)
    else -> Color(0xFFC62828)
}
