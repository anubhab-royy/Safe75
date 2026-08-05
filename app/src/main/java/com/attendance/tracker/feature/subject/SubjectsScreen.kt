package com.attendance.tracker.feature.subject

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.components.LoadingIndicator
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.subject.model.SubjectUiModel
import com.attendance.tracker.feature.subject.model.SubjectWithStats
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

/**
 * Screen displaying the list of all study subjects.
 * Allows searching, sorting, and launching editing or deletion dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    onNavigateToAddEditSubject: (Long) -> Unit,
    onNavigateToBackfill: (Long) -> Unit,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val subjectsState by viewModel.subjectsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var subjectToDelete by remember { mutableStateOf<SubjectWithStats?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Render Delete Confirmation Dialog if triggered
    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text(stringResource(R.string.delete_subject)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_2, subject.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubject(subject.id)
                        subjectToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Subjects",
                showBackButton = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEditSubject(-1L) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = Dimensions.SpacingMedium)
        ) {
            // Search and Sort Control Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text(stringResource(R.string.search_subjects)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort Options")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.alphabetical)) },
                            onClick = {
                                viewModel.onSortOptionChange(SubjectSortOption.ALPHABETICAL)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recently_added)) },
                            onClick = {
                                viewModel.onSortOptionChange(SubjectSortOption.RECENTLY_ADDED)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.attendance_goal)) },
                            onClick = {
                                viewModel.onSortOptionChange(SubjectSortOption.ATTENDANCE_GOAL)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Expose the proper UI flow depending on state
            when (val state = subjectsState) {
                is UiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.weight(1f))
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            EmptyState(
                                message = "Add your first subject.",
                                description = "Configure credit hours, targets, and schedule timings to get started tracking."
                            )
                            Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
                            TextButton(
                                onClick = { onNavigateToAddEditSubject(-1L) }
                            ) {
                                Text("Add Subject", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                            Text(
                                text = state.message ?: "An unexpected database error occurred",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = Dimensions.SpacingSmall),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                    ) {
                        items(state.data, key = { it.id }) { subject ->
                            SubjectCardItem(
                                subject = subject,
                                onClick = { onNavigateToAddEditSubject(subject.id) },
                                onDelete = { subjectToDelete = subject },
                                onBackfill = { onNavigateToBackfill(subject.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCardItem(
    subject: SubjectWithStats,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onBackfill: () -> Unit
) {
    val statusColor = when (subject.safetyStatus.uppercase(Locale.ROOT)) {
        "GOOD" -> Color(0xFF2E7D32)
        "WARNING" -> Color(0xFFFFA000)
        else -> Color(0xFFC62828)
    }

    if (subject.totalClasses == 0) {
        // Empty Attendance State: grey border/placeholder without percentage and progress bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Predefined Color Indicator Circle (smaller/secondary)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(subject.color))
                )

                Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    subject.faculty?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (subject.hasSchedules) "No attendance recorded yet" else "No attendance logged yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    if (subject.hasSchedules) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Backfill needed",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onBackfill() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Subject",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } else {
        // With attendance data card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
                    // Predefined Color Indicator Circle
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(subject.color))
                    )

                    Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        subject.faculty?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Trend Indicator placeholder (Reusable UI element)
                    val trendColor = when (subject.trend) {
                        "▲" -> Color(0xFF2E7D32)
                        "▼" -> Color(0xFFC62828)
                        else -> Color(0xFF757575)
                    }
                    val trendLabel = when (subject.trend) {
                        "▲" -> "▲ Improving"
                        "▼" -> "▼ Dropping"
                        else -> "● Stable"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(trendColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trendLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = trendColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Subject",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Percentage and Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", subject.percentage),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = "Goal: ${subject.attendanceGoal}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (subject.percentage / 100.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bunks left and Recovery message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bunkText = if (subject.safeMissCount > 0) {
                        "Bunks Left: ${subject.safeMissCount}"
                    } else {
                        "Bunks Left: 0"
                    }
                    Text(
                        text = bunkText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (subject.safeMissCount > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )

                    val recoveryText = if (subject.classesNeeded > 0) {
                        "Need: ${subject.classesNeeded}"
                    } else if (subject.classesNeeded == -1) {
                        "Goal unreachable"
                    } else {
                        "Goal reached"
                    }
                    Text(
                        text = recoveryText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (subject.classesNeeded > 0) Color(0xFFFFA000) else if (subject.classesNeeded == -1) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}
