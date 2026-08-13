package com.attendance.tracker.feature.schedule

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.attendance.tracker.domain.model.Schedule
import java.time.format.DateTimeFormatter

/**
 * Dialog explaining overlap conflicts between classes on the same day.
 */
@Composable
fun ConflictDialog(
    conflicts: List<Schedule>,
    subjectNameMapper: (Long) -> String,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val conflictInfo = conflicts.joinToString("\n") { conflict ->
        val name = subjectNameMapper(conflict.subjectId)
        "* $name on ${conflict.dayOfWeek.name} (${conflict.startTime.format(formatter)} - ${conflict.endTime.format(formatter)})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_overlap_conflict)) },
        text = {
            Text(stringResource(R.string.the_selected_timing_overlaps_with, conflictInfo))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.understood))
            }
        }
    )
}
