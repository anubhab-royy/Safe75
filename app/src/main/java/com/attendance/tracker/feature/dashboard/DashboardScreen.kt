package com.attendance.tracker.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.AttendanceStatistics
import com.attendance.tracker.feature.attendance.AttendanceViewModel
import com.attendance.tracker.feature.attendance.TodayScheduleItem
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Redesigned Dashboard screen serving as the main landing screen showing overall metrics
 * and today's scheduled classes to log attendance in real-time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAttendanceHistory: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val todayClasses by viewModel.todayClasses.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard",
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                // 1. Overall Statistics Panel
                overallStats?.let { stats ->
                    item {
                        StatsOverviewCard(
                            stats = stats,
                            onHistoryClick = onNavigateToAttendanceHistory
                        )
                    }
                }

                // 2. Today's Scheduled Classes header
                item {
                    Text(
                        text = "Today's Scheduled Classes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 3. Classes List
                if (todayClasses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                message = "No classes scheduled today",
                                description = "Enjoy your free day or configure schedules inside the Timetable tab."
                            )
                        }
                    }
                } else {
                    items(todayClasses, key = { it.scheduleId }) { item ->
                        TodayClassCard(
                            item = item,
                            onMark = { status ->
                                coroutineScope.launch {
                                    viewModel.markAttendance(
                                        subjectId = item.subjectId,
                                        scheduleId = item.scheduleId,
                                        status = status
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewCard(
    stats: AttendanceStatistics,
    onHistoryClick: () -> Unit
) {
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
                        text = String.format(Locale.getDefault(), "%.1f%%", stats.attendancePercentage),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = if (stats.attendancePercentage >= 75.0) {
                            Color(0xFF2E7D32) // Green
                        } else {
                            Color(0xFFC62828) // Red
                        }
                    )
                }
                OutlinedButton(
                    onClick = onHistoryClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("History logs")
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

            // Sub stats counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Present", value = stats.presentCount.toString(), color = Color(0xFF2E7D32))
                StatItem(label = "Absent", value = stats.absentCount.toString(), color = Color(0xFFC62828))
                StatItem(label = "Cancelled", value = stats.cancelledCount.toString(), color = Color(0xFF757575))
                StatItem(label = "Total", value = stats.totalClasses.toString(), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Safe missed limits and targets warnings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (stats.remainingSafeClasses > 0) {
                    Text(
                        text = "✓ You can safely miss the next ${stats.remainingSafeClasses} classes",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2E7D32)
                    )
                } else if (stats.attendancePercentage < 75.0) {
                    Text(
                        text = "⚠ Attendance is below 75% limit!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC62828)
                    )
                }

                if (stats.classesNeededToReachGoal > 0) {
                    Text(
                        text = "➡ Need ${stats.classesNeededToReachGoal} consecutive present classes to reach 85% goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                } else if (stats.classesNeededToReachGoal == -1) {
                    Text(
                        text = "Goal of 85% or above is mathematically unreachable due to absences",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
private fun TodayClassCard(
    item: TodayScheduleItem,
    onMark: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium)
        ) {
            // Subject color tag
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(item.subjectColor))
                    .align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.startTime} - ${item.endTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    item.room?.let {
                        Text(
                            text = "Room: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    item.faculty?.let {
                        Text(
                            text = "Teacher: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

                // Attendance quick logs
                if (item.attendance == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                    ) {
                        OutlinedButton(
                            onClick = { onMark(AttendanceStatus.PRESENT) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Present", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = { onMark(AttendanceStatus.ABSENT) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFC62828)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Absent", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = { onMark(AttendanceStatus.CANCELLED) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF616161)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    // Display status badge
                    val badgeColor = when (item.attendance.status) {
                        AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                        AttendanceStatus.ABSENT -> Color(0xFFC62828)
                        AttendanceStatus.CANCELLED -> Color(0xFF757575)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Logged as: ${item.attendance.status.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        )
                    }
                }
            }
        }
    }
}
