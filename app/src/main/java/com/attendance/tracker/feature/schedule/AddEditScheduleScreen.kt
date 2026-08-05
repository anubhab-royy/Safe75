package com.attendance.tracker.feature.schedule

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Screen providing the schedule creation and modification form.
 * Uses Material 3 TimePicker to select class slot bounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScheduleScreen(
    scheduleId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToBackfill: (Long) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val validationState by viewModel.validationState.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val pendingBackfill by viewModel.pendingBackfillPrompt.collectAsStateWithLifecycle()

    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedDays by remember { mutableStateOf(setOf<WeekDay>()) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var room by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }

    var isLoaded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var showSubjectDropdown by remember { mutableStateOf(false) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Load initial schedule configs in Edit Mode
    LaunchedEffect(scheduleId, subjects) {
        if (scheduleId != -1L && !isLoaded && subjects.isNotEmpty()) {
            val schedule = viewModel.getScheduleById(scheduleId)
            if (schedule != null) {
                selectedSubject = subjects.find { it.id == schedule.subjectId }
                selectedDays = setOf(schedule.dayOfWeek)
                startTime = schedule.startTime
                endTime = schedule.endTime
                room = schedule.room ?: ""
                teacher = schedule.teacherOverride ?: ""
            }
            isLoaded = true
        } else if (scheduleId == -1L && subjects.isNotEmpty() && selectedSubject == null) {
            selectedSubject = subjects.first()
            isLoaded = true
        }
    }

    fun hasChanges(): Boolean {
        return room.isNotBlank() || teacher.isNotBlank() || startTime != LocalTime.of(9, 0) || endTime != LocalTime.of(10, 0) || selectedDays.isNotEmpty()
    }

    val onBackRequest = {
        if (hasChanges()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = onBackRequest)

    // Unsaved Changes Alert Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.you_have_unsaved_changes_are)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.clearValidationError()
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Overlap Conflicts Alert Dialog
    if (conflicts.isNotEmpty()) {
        ConflictDialog(
            conflicts = conflicts,
            subjectNameMapper = { id ->
                subjects.find { it.id == id }?.name ?: "Unknown Subject"
            },
            onDismiss = { viewModel.clearValidationError() }
        )
    }

    // Backfill Prompt Dialog (surfaced after saving a schedule with missing past attendance)
    pendingBackfill?.let { prompt ->
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissBackfillPrompt()
                onNavigateBack()
            },
            title = { Text(stringResource(R.string.backfill_attendance)) },
            text = {
                Text(
                    "${prompt.subjectName} has ${prompt.missingCount} past " +
                            "${if (prompt.missingCount == 1) "class" else "classes"} without attendance. " +
                            "Would you like to fill them now?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val subjectId = prompt.subjectId
                        viewModel.dismissBackfillPrompt()
                        onNavigateToBackfill(subjectId)
                    }
                ) {
                    Text(stringResource(R.string.backfill_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissBackfillPrompt()
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    // Start Time M3 TimePicker Dialog
    if (showStartTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        showStartTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.select_start_time)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            }
        )
    }

    // End Time M3 TimePicker Dialog
    if (showEndTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        showEndTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.select_end_time)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (scheduleId == -1L) "Add Class Slot" else "Edit Class Slot",
                showBackButton = true,
                onBackClick = onBackRequest
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(Dimensions.SpacingMedium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            // Subject Dropdown Selector
            Text("Subject", style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedSubject?.name ?: "Select a Subject",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSubjectDropdown = true },
                    enabled = false,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                DropdownMenu(
                    expanded = showSubjectDropdown,
                    onDismissRequest = { showSubjectDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                selectedSubject = subject
                                showSubjectDropdown = false
                            }
                        )
                    }
                }
            }

            // Days of the Week Multi-Select Picker
            Text("Days of the Week", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                WeekDay.ordered.forEach { day ->
                    val isSelected = day in selectedDays
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        label = { Text(day.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            if (selectedDays.isEmpty()) {
                Text(
                    text = "Select at least one day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Time Selector Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start Time", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(startTime.format(timeFormatter))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("End Time", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(endTime.format(timeFormatter))
                    }
                }
            }

            // Room Location TextField
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(stringResource(R.string.room_lab_name_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Teacher Override Name TextField
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text(stringResource(R.string.teacher_override_name_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // Validation Error Panel
            if (validationState is ValidationResult.Invalid) {
                Text(
                    text = (validationState as ValidationResult.Invalid).reason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                OutlinedButton(
                    onClick = onBackRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val subId = selectedSubject?.id ?: return@Button
                        coroutineScope.launch {
                            val success = viewModel.saveScheduleMultiDay(
                                id = if (scheduleId == -1L) 0L else scheduleId,
                                subjectId = subId,
                                days = selectedDays,
                                startTime = startTime,
                                endTime = endTime,
                                room = room,
                                teacher = teacher
                            )
                            if (success) {
                                viewModel.clearValidationError()
                                // If missing historical attendance was detected the prompt
                                // dialog drives navigation; otherwise return to the schedule list.
                                if (viewModel.pendingBackfillPrompt.value == null) {
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    enabled = selectedDays.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
