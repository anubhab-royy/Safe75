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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.isSystemInDarkTheme
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
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val subjectStatsList by viewModel.subjectStatsList.collectAsState()
    val todayClasses by viewModel.todayClasses.collectAsState()
    val goal by viewModel.attendanceGoal.collectAsState()

    var expandedCards by remember { mutableStateOf(emptySet<Long>()) }

    val pendingCount = todayClasses.count { it.attendance == null }

    Scaffold(
        topBar = {
            AppTopBar(
                title = activeVersion?.let { "Dashboard (${it.name})" } ?: "Dashboard",
                showBackButton = false
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimensions.SpacingMedium,
                end = Dimensions.SpacingMedium,
                top = Dimensions.SpacingMedium,
                bottom = Dimensions.SpacingLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            // 1. Section: Overall Stats
            item {
                Text(
                    text = "Overall Stats",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            dashboardStats?.let { stats ->
                item {
                    OverallStatisticsCard(stats = stats, goal = goal)
                }
            }

            // Divider 1
            item {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 2. Section: Mark Your Attendance
            item {
                Text(
                    text = "Mark Your Attendance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (pendingCount > 0) {
                item {
                    PendingAttendanceReminderCard(count = pendingCount)
                }
            }
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
                    val isExpanded = expandedCards.contains(item.scheduleId)
                    TodayClassCard(
                        item = item,
                        isExpanded = isExpanded,
                        onEditClick = {
                            expandedCards = if (isExpanded) {
                                expandedCards - item.scheduleId
                            } else {
                                expandedCards + item.scheduleId
                            }
                        },
                        onStatusClick = { newStatus ->
                            viewModel.onAttendanceStatusClick(item, newStatus)
                            // Auto collapse after marking
                            if (isExpanded) {
                                expandedCards = expandedCards - item.scheduleId
                            }
                        }
                    )
                }
            }

            // Divider 2
            item {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 3. Section: Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                QuickActionsCard(
                    onHistoryClick = onNavigateToAttendanceHistory,
                    onSimulatorClick = onNavigateToSimulator,
                    onPlannerClick = onNavigateToLeavePlanner
                )
            }

            // Divider 3
            item {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 4. Section: Needs Attention
            item {
                Text(
                    text = "Needs Attention",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            val atRiskSubjects = subjectStatsList.filter { it.totalClasses > 0 && it.percentage < goal }
            if (atRiskSubjects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ All subjects are currently on track.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            } else {
                items(atRiskSubjects.take(3), key = { it.subjectId }) { subjectStats ->
                    NeedsAttentionCard(stats = subjectStats, goal = goal)
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
    isExpanded: Boolean,
    onEditClick: () -> Unit,
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
            if (status != null && !isExpanded) {
                // Collapsed State for completed class
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.subjectName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.startTime} – ${item.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusText = when (status) {
                            AttendanceStatus.PRESENT -> "✓ Present"
                            AttendanceStatus.ABSENT -> "✗ Absent"
                            AttendanceStatus.CANCELLED -> "Cancelled"
                        }
                        val statusColor = when (status) {
                            AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                            AttendanceStatus.ABSENT -> Color(0xFFC62828)
                            AttendanceStatus.CANCELLED -> Color(0xFF757575)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )

                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .clickable { onEditClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Expanded/Pending State
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Text(
                            text = "Collapse",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .clickable { onEditClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
private fun OverallStatisticsCard(stats: DashboardStatistics, goal: Double) {
    val hasNoData = stats.totalClasses == 0
    val isDark = isSystemInDarkTheme()

    val statusText = if (hasNoData) "No Data" else when (stats.safetyStatus.uppercase(Locale.ROOT)) {
        "GOOD" -> "Good"
        "WARNING" -> "Near Goal"
        else -> "Critical"
    }

    val (backgroundColor, contentColor, statusColor) = if (hasNoData) {
        Triple(
            if (isDark) Color(0xFF37474F).copy(alpha = 0.2f) else Color(0xFFECEFF1),
            if (isDark) Color(0xFFB0BEC5) else Color(0xFF37474F),
            if (isDark) Color(0xFFB0BEC5) else Color(0xFF455A64)
        )
    } else {
        when (stats.safetyStatus.uppercase(Locale.ROOT)) {
            "GOOD" -> {
                Triple(
                    if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9),
                    if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20),
                    if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                )
            }
            "WARNING" -> {
                Triple(
                    if (isDark) Color(0xFFE65100).copy(alpha = 0.15f) else Color(0xFFFFF3E0),
                    if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                    if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                )
            }
            else -> { // CRITICAL
                Triple(
                    if (isDark) Color(0xFFB71C1C).copy(alpha = 0.15f) else Color(0xFFFFEBEE),
                    if (isDark) Color(0xFFEF9A9A) else Color(0xFFB71C1C),
                    if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
                )
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overall Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    if (hasNoData) {
                        Text(
                            text = "No attendance recorded",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", stats.overallPercentage),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 48.sp
                            ),
                            color = statusColor
                        )
                    }
                    Text(
                        text = "Goal: ${goal.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                // Safety Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
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
                CountItem(label = "Present", value = if (hasNoData) "-" else stats.presentCount.toString(), color = Color(0xFF2E7D32), contentColor = contentColor)
                CountItem(label = "Absent", value = if (hasNoData) "-" else stats.absentCount.toString(), color = Color(0xFFC62828), contentColor = contentColor)
                CountItem(label = "Cancelled", value = if (hasNoData) "-" else stats.cancelledCount.toString(), color = Color(0xFF757575), contentColor = contentColor)
                
                // Total classes (Secondary Information, smaller text)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasNoData) "-" else stats.totalClasses.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlighted recommendation banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (recIcon, recMessage) = when {
                    hasNoData -> Pair(Icons.Default.Info, "Mark past attendance or log today's classes to begin tracking.")
                    stats.safeMissCount > 0 -> Pair(Icons.Default.Info, "You may safely miss the next ${stats.safeMissCount} classes")
                    stats.classesNeeded > 0 -> Pair(Icons.Default.Warning, "Attend next ${stats.classesNeeded} classes to reach ${goal.toInt()}% goal")
                    stats.classesNeeded == -1 -> Pair(Icons.Default.Warning, "Goal is mathematically unreachable")
                    else -> Pair(Icons.Default.Info, "✓ Goal achieved! Keep it up.")
                }
                Icon(
                    imageVector = recIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun CountItem(label: String, value: String, color: Color, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun QuickActionsCard(
    onHistoryClick: () -> Unit,
    onSimulatorClick: () -> Unit,
    onPlannerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.List,
            label = "Logs History",
            onClick = onHistoryClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.PlayArrow,
            label = "Simulator",
            onClick = onSimulatorClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.DateRange,
            label = "Leave Planner",
            onClick = onPlannerClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NeedsAttentionCard(stats: SubjectStatistics, goal: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stats.subjectName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", stats.percentage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal: ${goal.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                val recoveryMsg = when (stats.classesNeeded) {
                    -1 -> "Goal unreachable"
                    else -> "Need ${stats.classesNeeded} consecutive presents"
                }
                Text(
                    text = recoveryMsg,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
