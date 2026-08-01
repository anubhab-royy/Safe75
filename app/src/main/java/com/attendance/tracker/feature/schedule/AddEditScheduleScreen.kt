package com.attendance.tracker.feature.schedule

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
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
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val subjects by viewModel.subjects.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()

    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedDay by remember { mutableStateOf(WeekDay.Monday) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var room by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }

    var isLoaded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showDayDropdown by remember { mutableStateOf(false) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Load initial schedule configs in Edit Mode
    LaunchedEffect(scheduleId, subjects) {
        if (scheduleId != -1L && !isLoaded && subjects.isNotEmpty()) {
            val schedule = viewModel.getScheduleById(scheduleId)
            if (schedule != null) {
                selectedSubject = subjects.find { it.id == schedule.subjectId }
                selectedDay = schedule.dayOfWeek
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
        return room.isNotBlank() || teacher.isNotBlank() || startTime != LocalTime.of(9, 0) || endTime != LocalTime.of(10, 0)
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
            title = { Text("Discard Changes") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
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
                    Text("Cancel")
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Start Time") },
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select End Time") },
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

            // Day of Week Dropdown Selector
            Text("Day of the Week", style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDay.name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDayDropdown = true },
                    enabled = false,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                DropdownMenu(
                    expanded = showDayDropdown,
                    onDismissRequest = { showDayDropdown = false }
                ) {
                    WeekDay.values().forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day.name) },
                            onClick = {
                                selectedDay = day
                                showDayDropdown = false
                            }
                        )
                    }
                }
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
                label = { Text("Room / Lab Name (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Teacher Override Name TextField
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("Teacher Override Name (Optional)") },
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
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val subId = selectedSubject?.id ?: return@Button
                        coroutineScope.launch {
                            val success = viewModel.saveSchedule(
                                id = if (scheduleId == -1L) 0L else scheduleId,
                                subjectId = subId,
                                day = selectedDay,
                                startTime = startTime,
                                endTime = endTime,
                                room = room,
                                teacher = teacher
                            )
                            if (success) {
                                viewModel.clearValidationError()
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
