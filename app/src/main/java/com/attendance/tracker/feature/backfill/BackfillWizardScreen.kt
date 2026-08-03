package com.attendance.tracker.feature.backfill

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import java.time.format.DateTimeFormatter

/**
 * Backfill Attendance Wizard.
 *
 * Lists every past class that still has no attendance record, grouped by date.
 * Each entry can be marked Present / Absent / Cancelled (tapping the active
 * status again removes the record), and bulk actions mark every remaining
 * entry in one tap. Changes are saved immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackfillWizardScreen(
    subjectId: Long,
    onNavigateBack: () -> Unit,
    viewModel: BackfillWizardViewModel = hiltViewModel()
) {
    val items by viewModel.missingItems.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(subjectId) {
        viewModel.setSubjectFilter(subjectId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Backfill Attendance",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Header: pending count + bulk actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} ${if (items.size == 1) "class needs" else "classes need"} attendance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    BulkActionButton(
                        label = "All Present",
                        color = Color(0xFF2E7D32),
                        onClick = { viewModel.markAll(AttendanceStatus.PRESENT) },
                        modifier = Modifier.weight(1f)
                    )
                    BulkActionButton(
                        label = "All Absent",
                        color = Color(0xFFC62828),
                        onClick = { viewModel.markAll(AttendanceStatus.ABSENT) },
                        modifier = Modifier.weight(1f)
                    )
                    BulkActionButton(
                        label = "All Cancelled",
                        color = Color(0xFF757575),
                        onClick = { viewModel.markAll(AttendanceStatus.CANCELLED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyState(
                        message = "All caught up!",
                        description = "No past classes are missing attendance."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Dimensions.SpacingMedium,
                        end = Dimensions.SpacingMedium,
                        top = Dimensions.SpacingSmall,
                        bottom = Dimensions.SpacingMedium
                    )
                ) {
                    val grouped = items.groupBy { it.date }.toSortedMap()
                    grouped.forEach { (date, dateItems) ->
                        item(key = "header-$date") {
                            Text(
                                text = date.format(dateFormatter),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = Dimensions.SpacingSmall)
                            )
                        }
                        items(dateItems, key = { "${it.scheduleId}-${it.date}" }) { item ->
                            BackfillWizardRow(
                                item = item,
                                timeFormatter = timeFormatter,
                                onMarkStatus = { status -> viewModel.markAttendance(item, status) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Done
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingMedium)
                    .height(48.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BulkActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BackfillWizardRow(
    item: BackfillWizardItem,
    timeFormatter: DateTimeFormatter,
    onMarkStatus: (AttendanceStatus) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.SpacingExtraSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(item.subjectColor))
                )
                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subjectName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${item.dayOfWeek.name}, ${item.startTime.format(timeFormatter)} - ${item.endTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.room?.let { room ->
                        Text(
                            text = "Room: $room",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                StatusButton(
                    label = "Present",
                    color = Color(0xFF2E7D32),
                    onClick = { onMarkStatus(AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    label = "Absent",
                    color = Color(0xFFC62828),
                    onClick = { onMarkStatus(AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    label = "Cancelled",
                    color = Color(0xFF757575),
                    onClick = { onMarkStatus(AttendanceStatus.CANCELLED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = color
        )
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
