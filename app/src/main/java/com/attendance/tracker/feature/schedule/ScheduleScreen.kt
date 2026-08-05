package com.attendance.tracker.feature.schedule

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.components.LoadingIndicator
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.schedule.model.ScheduleUiModel

/**
 * Main timetable view displaying daily schedule lists, day tabs, search queries,
 * and timetable version settings panels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToAddEditSchedule: (Long) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeVersion by viewModel.activeVersion.collectAsStateWithLifecycle()
    val versions by viewModel.versions.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val days = WeekDay.values()

    var showVersionMenu by remember { mutableStateOf(false) }
    var showManageVersionsDialog by remember { mutableStateOf(false) }
    var showCreateVersionDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleUiModel?>(null) }

    // Deletion Confirmation Dialog
    scheduleToDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text(stringResource(R.string.delete_class_slot)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_1, schedule.subjectName, schedule.dayOfWeek, schedule.startTime)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(schedule)
                        scheduleToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Manage Versions Dialog
    if (showManageVersionsDialog) {
        ManageVersionsDialog(
            versions = versions,
            activeVersionId = activeVersion?.id ?: -1L,
            onSwitch = { viewModel.switchActiveVersion(it) },
            onDelete = { viewModel.deleteTimetableVersion(it) },
            onRename = { id, name -> viewModel.renameTimetableVersion(id, name) },
            onCreateNew = { showCreateVersionDialog = true },
            onDismiss = { showManageVersionsDialog = false }
        )
    }

    // Create New Version Dialog
    if (showCreateVersionDialog) {
        var versionInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateVersionDialog = false },
            title = { Text(stringResource(R.string.create_timetable_version)) },
            text = {
                OutlinedTextField(
                    value = versionInput,
                    onValueChange = { versionInput = it },
                    label = { Text(stringResource(R.string.version_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (versionInput.isNotBlank()) {
                            viewModel.createTimetableVersion(versionInput)
                            showCreateVersionDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateVersionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Timetable",
                showBackButton = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEditSchedule(-1L) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class Slot")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Timetable Version Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showVersionMenu = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeVersion?.name ?: "No Active Version",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Version",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showVersionMenu,
                        onDismissRequest = { showVersionMenu = false }
                    ) {
                        versions.forEach { version ->
                            DropdownMenuItem(
                                text = { Text(version.name) },
                                onClick = {
                                    viewModel.switchActiveVersion(version.id)
                                    showVersionMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { showManageVersionsDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Manage Timetable Versions",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Real-time Text Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text(stringResource(R.string.search_by_subject_room_or)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                shape = MaterialTheme.shapes.medium
            )

            // Weekly day selectors
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = Dimensions.SpacingMedium
            ) {
                days.forEachIndexed { index, weekday ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(weekday.name) }
                    )
                }
            }

            // Timetable Daily List
            when (val state = uiState) {
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
                        EmptyState(
                            message = "No classes scheduled",
                            description = "Add class slots to map your weekly schedules."
                        )
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                            Text(text = state.message ?: "Failed to load schedules")
                        }
                    }
                }
                is UiState.Success -> {
                    val todaySchedules = state.data.filter {
                        it.dayOfWeek.equals(days[selectedTabIndex].name, ignoreCase = true)
                    }

                    if (todaySchedules.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                message = "Free Day!",
                                description = "No classes configured for this day."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(Dimensions.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                        ) {
                            items(todaySchedules, key = { it.id }) { schedule ->
                                DayScheduleCard(
                                    schedule = schedule,
                                    onClick = { onNavigateToAddEditSchedule(schedule.id) },
                                    onDelete = { scheduleToDelete = schedule }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    schedule: ScheduleUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(schedule.subjectColor))
            )

            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.subjectName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    schedule.room?.let {
                        Text(
                            text = "Room: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    schedule.faculty?.let {
                        Text(
                            text = "Teacher: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Slot",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ManageVersionsDialog(
    versions: List<com.attendance.tracker.domain.model.SemesterVersion>,
    activeVersionId: Long,
    onSwitch: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    var editingVersionId by remember { mutableStateOf<Long?>(null) }
    var renameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.manage_timetable_versions))
                TextButton(onClick = onCreateNew) {
                    Text(stringResource(R.string.new_btn))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    items(versions) { version ->
                        val isActive = version.id == activeVersionId
                        val isEditing = version.id == editingVersionId

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitch(version.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                                    Text(
                                        text = version.name,
                                        style = if (isActive) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingVersionId = version.id
                                            renameInput = version.name
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDelete(version.id) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = renameInput,
                                        onValueChange = { renameInput = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            onRename(version.id, renameInput)
                                            editingVersionId = null
                                        }
                                    ) {
                                        Text(stringResource(R.string.save))
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
