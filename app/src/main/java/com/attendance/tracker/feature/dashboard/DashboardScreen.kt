package com.attendance.tracker.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.attendance.tracker.core.model.AttendanceStatus
import java.time.LocalTime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.DashboardStatistics
import com.attendance.tracker.domain.model.SubjectStatistics
import com.attendance.tracker.feature.attendance.TodayScheduleItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAttendanceHistory: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToLeavePlanner: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val activeVersion by viewModel.activeVersion.collectAsState()
    val filterScope by viewModel.filter.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val subjectStatsList by viewModel.subjectStatsList.collectAsState()
    val upcomingClass by viewModel.upcomingClass.collectAsState()
    val todayClasses by viewModel.todayClasses.collectAsState()

    val pendingCount = todayClasses.count { it.attendance == null }

    Scaffold(
        topBar = {
            AppTopBar(
                title = activeVersion?.let { "Dashboard (${it.name})" } ?: "Dashboard",
                showBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Filter chip selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                DashboardFilter.values().forEach { filterVal ->
                    FilterChip(
                        selected = filterScope == filterVal,
                        onClick = { viewModel.onFilterScopeChange(filterVal) },
                        label = { Text(filterVal.name) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Dimensions.SpacingMedium,
                    end = Dimensions.SpacingMedium,
                    bottom = Dimensions.SpacingLarge
                ),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                // 1. Pending Attendance Reminder
                if (pendingCount > 0) {
                    item {
                        PendingAttendanceReminderCard(count = pendingCount)
                    }
                }

                // 2. Today's Classes Header
                item {
                    Text(
                        text = "Today's Classes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 3. Today's Classes List (Empty State or Cards)
                if (todayClasses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimensions.SpacingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No classes scheduled today.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(todayClasses, key = { it.scheduleId }) { item ->
                        TodayClassCard(
                            item = item,
                            onStatusClick = { newStatus ->
                                viewModel.onAttendanceStatusClick(item, newStatus)
                            }
                        )
                    }
                }

                // 4. Section Header: Attendance Summary
                item {
                    Text(
                        text = "Attendance Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 5. Overall Statistics Card
                dashboardStats?.let { stats ->
                    item {
                        OverallStatisticsCard(stats = stats)
                    }
                }

                // 6. Upcoming Class Alert Card
                upcomingClass?.let { item ->
                    item {
                        UpcomingClassCard(item = item)
                    }
                }

                // 7. Quick Actions Card
                item {
                    QuickActionsCard(
                        onHistoryClick = onNavigateToAttendanceHistory,
                        onSimulatorClick = onNavigateToSimulator,
                        onPlannerClick = onNavigateToLeavePlanner
                    )
                }

                // 8. Section Header: Subjects
                item {
                    Text(
                        text = "Subject-wise Analytics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 9. Subject Summary cards list
                if (subjectStatsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                message = "No subjects logged yet",
                                description = "Navigate to the Subjects tab to record your modules."
                            )
                        }
                    }
                } else {
                    items(subjectStatsList, key = { it.subjectId }) { subjectStats ->
                        SubjectStatisticsCard(stats = subjectStats)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAttendanceReminderCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))
            Text(
                text = "You have $count ${if (count == 1) "class" else "classes"} pending.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TodayClassCard(
    item: TodayScheduleItem,
    onStatusClick: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val attendance = item.attendance
    val status = attendance?.status

    val cardColor = if (status != null) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Subject & Time Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Time Awareness Badge
                val now = LocalTime.now()
                val start = LocalTime.parse(item.startTime)
                val end = LocalTime.parse(item.endTime)
                val (badgeText, badgeColor) = when {
                    status != null -> {
                        "Completed" to Color(0xFF2E7D32)
                    }
                    now.isBefore(start) -> {
                        val diffMin = java.time.Duration.between(now, start).toMinutes()
                        val text = if (diffMin < 60) "Starts in $diffMin min" else "Starts at ${item.startTime}"
                        text to MaterialTheme.colorScheme.primary
                    }
                    now.isAfter(start) && now.isBefore(end) -> {
                        "Class in progress" to Color(0xFFFFA000)
                    }
                    else -> {
                        "Class ended" to Color(0xFFC62828)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Details: Time, Room, Faculty
            Text(
                text = "${item.startTime} – ${item.endTime}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            item.room?.let { room ->
                Text(
                    text = "Room: $room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.faculty?.let { faculty ->
                Text(
                    text = "Faculty: $faculty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attendance Status Label
            if (status != null) {
                Text(
                    text = "✓ Attendance Recorded: ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                            AttendanceStatus.ABSENT -> Color(0xFFC62828)
                            AttendanceStatus.CANCELLED -> Color(0xFF757575)
                        }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceButton(
                    label = "Present",
                    isSelected = status == AttendanceStatus.PRESENT,
                    isAnySelected = status != null,
                    onClick = { onStatusClick(AttendanceStatus.PRESENT) },
                    selectedColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                AttendanceButton(
                    label = "Absent",
                    isSelected = status == AttendanceStatus.ABSENT,
                    isAnySelected = status != null,
                    onClick = { onStatusClick(AttendanceStatus.ABSENT) },
                    selectedColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
                AttendanceButton(
                    label = "Cancelled",
                    isSelected = status == AttendanceStatus.CANCELLED,
                    isAnySelected = status != null,
                    onClick = { onStatusClick(AttendanceStatus.CANCELLED) },
                    selectedColor = Color(0xFF757575),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceButton(
    label: String,
    isSelected: Boolean,
    isAnySelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) {
        Color.White
    } else if (isAnySelected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        selectedColor
    }

    val containerColor = if (isSelected) {
        selectedColor
    } else {
        Color.Transparent
    }

    val border = if (isSelected) {
        null
    } else {
        androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isAnySelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.38f) else selectedColor
        )
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = border,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverallStatisticsCard(stats: DashboardStatistics) {
    val statusColor = when (stats.safetyStatus) {
        "SAFE" -> Color(0xFF2E7D32)      // Green
        "WARNING" -> Color(0xFFFFA000)   // Amber
        else -> Color(0xFFC62828)        // Red
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", stats.overallPercentage),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp
                        ),
                        color = statusColor
                    )
                }

                // Safety Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stats.safetyStatus,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

            // Sub counts metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CountItem(label = "Present", value = stats.presentCount.toString(), color = Color(0xFF2E7D32))
                CountItem(label = "Absent", value = stats.absentCount.toString(), color = Color(0xFFC62828))
                CountItem(label = "Cancelled", value = stats.cancelledCount.toString(), color = Color(0xFF757575))
                CountItem(label = "Total", value = stats.totalClasses.toString(), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Intelligent recommendations bunk targets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.SpacingSmall)
            ) {
                if (stats.safeMissCount > 0) {
                    Text(
                        text = "You may safely miss the next ${stats.safeMissCount} classes",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                } else if (stats.classesNeeded > 0) {
                    Text(
                        text = "Attend next ${stats.classesNeeded} classes to reach 85% goal",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFA000)
                    )
                } else if (stats.classesNeeded == -1) {
                    Text(
                        text = "Goal is mathematically unreachable due to absences",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun QuickActionsCard(
    onHistoryClick: () -> Unit,
    onSimulatorClick: () -> Unit,
    onPlannerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                OutlinedButton(onClick = onHistoryClick, modifier = Modifier.weight(1f)) {
                    Text("Logs History", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onSimulatorClick, modifier = Modifier.weight(1f)) {
                    Text("Simulator", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onPlannerClick, modifier = Modifier.weight(1f)) {
                    Text("Leave Planner", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun UpcomingClassCard(item: TodayScheduleItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upcoming Class",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${item.startTime} - ${item.endTime} (Room: ${item.room ?: "N/A"})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SubjectStatisticsCard(stats: SubjectStatistics) {
    val statusColor = when (stats.safetyStatus) {
        "SAFE" -> Color(0xFF2E7D32)
        "WARNING" -> Color(0xFFFFA000)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            // Subject color tag
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Color(stats.subjectColor))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimensions.SpacingMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stats.subjectName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Safety badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stats.safetyStatus,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%% (%d/%d classes)", stats.percentage, stats.presentCount, stats.totalClasses),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                    Text(
                        text = "Goal: ${stats.personalGoalPercentage}% (Req: ${stats.requiredPercentage}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bunk guidance metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stats.safeMissCount > 0) {
                        Text(
                            text = "Bunks left: ${stats.safeMissCount}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "Bunks left: 0",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }

                    if (stats.classesNeeded > 0) {
                        Text(
                            text = "Needed: ${stats.classesNeeded}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFA000)
                        )
                    } else if (stats.classesNeeded == -1) {
                        Text(
                            text = "Goal unreachable",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    } else {
                        Text(
                            text = "Goal reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}
