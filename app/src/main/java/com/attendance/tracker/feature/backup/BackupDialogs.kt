package com.attendance.tracker.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendance.tracker.core.backup.BackupData
import com.attendance.tracker.domain.model.RestoreOptions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shows the parsed contents of a backup file before any data is committed.
 * Displays metadata, version information, and per-category record counts.
 *
 * @param onContinue Optional action shown as "Continue to Restore" once the
 *                   user has reviewed the preview. When null only a Close button is shown.
 */
@Composable
fun BackupPreviewDialog(
    previewData: BackupData,
    onDismiss: () -> Unit,
    onContinue: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Preview") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val meta = previewData.metadata
                Text("Created: ${formatTimestamp(meta.createdAt)}", style = MaterialTheme.typography.bodySmall)
                Text("App version: ${meta.appVersion}", style = MaterialTheme.typography.bodySmall)
                Text("Schema version: ${meta.schemaVersion}", style = MaterialTheme.typography.bodySmall)
                Text("Backup format: ${meta.backupVersion}", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(4.dp))
                Text("Contents:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("• ${previewData.subjects.size} subjects", style = MaterialTheme.typography.bodyMedium)
                Text("• ${previewData.schedules.size} schedules", style = MaterialTheme.typography.bodyMedium)
                Text("• ${previewData.semesterVersions.size} semester versions", style = MaterialTheme.typography.bodyMedium)
                Text("• ${previewData.attendanceRecords.size} attendance records", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(4.dp))
                Text(
                    "Review the details above. Nothing is written to your database until you confirm the restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (onContinue != null) {
                Button(onClick = onContinue) { Text("Continue to Restore") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Lets the user choose which data categories to restore (selective restore)
 * before the import is committed.
 *
 * @param options              Current [RestoreOptions] selection.
 * @param onOptionsChanged     Callback when a checkbox toggles.
 * @param onConfirm            Callback to commit the restore.
 * @param onDismiss            Callback to dismiss without restoring.
 * @param subjectCount         Number of subjects in the source data.
 * @param scheduleCount        Number of schedules in the source data.
 * @param attendanceCount      Number of attendance records in the source data.
 * @param semesterVersionCount Number of semester versions in the source data.
 * @param showSemesterVersions Whether the semester-version checkbox is shown.
 * @param showSettings         Whether the settings checkbox is shown.
 * @param showPreviewSummary   Whether the summary card is shown above the options.
 * @param previewTitle         Title of the summary card (if shown).
 */
@Composable
fun RestoreOptionsDialog(
    options: RestoreOptions,
    onOptionsChanged: (RestoreOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    subjectCount: Int = 0,
    scheduleCount: Int = 0,
    attendanceCount: Int = 0,
    semesterVersionCount: Int = 0,
    showSemesterVersions: Boolean = true,
    showSettings: Boolean = true,
    showPreviewSummary: Boolean = true,
    previewTitle: String = "Backup Preview"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Preview summary
                if (showPreviewSummary) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(previewTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("$subjectCount subjects • $scheduleCount schedules", style = MaterialTheme.typography.bodySmall)
                            Text("$attendanceCount attendance records", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text("Select data to restore:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                RestoreCheckbox("Subjects ($subjectCount)", options.restoreSubjects) {
                    onOptionsChanged(options.copy(restoreSubjects = it))
                }
                RestoreCheckbox("Schedules ($scheduleCount)", options.restoreSchedules) {
                    onOptionsChanged(options.copy(restoreSchedules = it))
                }
                if (showSemesterVersions) {
                    RestoreCheckbox("Semester Versions ($semesterVersionCount)", options.restoreSemesterVersions) {
                        onOptionsChanged(options.copy(restoreSemesterVersions = it))
                    }
                }
                RestoreCheckbox("Attendance Records ($attendanceCount)", options.restoreAttendance) {
                    onOptionsChanged(options.copy(restoreAttendance = it))
                }
                if (showSettings) {
                    RestoreCheckbox("Settings", options.restoreSettings) {
                        onOptionsChanged(options.copy(restoreSettings = it))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Convenience overload for restoring from a parsed backup file preview.
 */
@Composable
fun RestoreOptionsDialog(
    previewData: BackupData,
    options: RestoreOptions,
    onOptionsChanged: (RestoreOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    RestoreOptionsDialog(
        options = options,
        onOptionsChanged = onOptionsChanged,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        subjectCount = previewData.subjects.size,
        scheduleCount = previewData.schedules.size,
        attendanceCount = previewData.attendanceRecords.size,
        semesterVersionCount = previewData.semesterVersions.size,
        showSemesterVersions = true,
        showSettings = true,
        showPreviewSummary = true
    )
}

@Composable
private fun RestoreCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "unknown"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
