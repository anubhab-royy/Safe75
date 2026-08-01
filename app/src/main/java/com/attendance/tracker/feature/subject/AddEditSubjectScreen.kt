package com.attendance.tracker.feature.subject

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.core.ui.theme.Radius
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.launch

/**
 * Predefined Material colors for the subject color picker.
 */
val PredefinedColors = listOf(
    0xFFE57373.toInt(), // Coral Red
    0xFFF06292.toInt(), // Pastel Pink
    0xFFBA68C8.toInt(), // Lavender Violet
    0xFF7986CB.toInt(), // Periwinkle Blue
    0xFF64B5F6.toInt(), // Sky Blue
    0xFF4DB6AC.toInt(), // Mint Teal
    0xFF81C784.toInt(), // Soft Green
    0xFFFFD54F.toInt(), // Amber Gold
    0xFFFFB74D.toInt(), // Soft Orange
    0xFFA1887F.toInt()  // Soft Brown
)

/**
 * Screen for creating or updating a study subject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(
    subjectId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val validationState by viewModel.validationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var requiredAttendance by remember { mutableIntStateOf(75) }
    var personalGoal by remember { mutableIntStateOf(85) }
    var selectedColor by remember { mutableIntStateOf(PredefinedColors[0]) }

    var isLoaded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Load initial values for Edit mode
    LaunchedEffect(subjectId) {
        if (subjectId != -1L && !isLoaded) {
            val subject = viewModel.getSubjectById(subjectId)
            if (subject != null) {
                name = subject.name
                faculty = subject.faculty ?: ""
                requiredAttendance = subject.requiredAttendance
                personalGoal = subject.attendanceGoal
                selectedColor = if (subject.color in PredefinedColors) subject.color else PredefinedColors[0]
            }
            isLoaded = true
        } else {
            isLoaded = true
        }
    }

    // Determine if any form inputs have changed
    fun hasChanges(): Boolean {
        return name.isNotBlank() || faculty.isNotBlank() || requiredAttendance != 75 || personalGoal != 85 || selectedColor != PredefinedColors[0]
    }

    val onBackRequest = {
        if (hasChanges()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Handle system back press
    BackHandler(enabled = true, onBack = onBackRequest)

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (subjectId == -1L) "Add Subject" else "Edit Subject",
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
            // Subject Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                isError = validationState is ValidationResult.Invalid && 
                        (validationState as ValidationResult.Invalid).reason.contains("name", ignoreCase = true)
            )

            // Faculty Name Input
            OutlinedTextField(
                value = faculty,
                onValueChange = { faculty = it },
                label = { Text("Faculty / Teacher Name (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                isError = validationState is ValidationResult.Invalid && 
                        (validationState as ValidationResult.Invalid).reason.contains("faculty", ignoreCase = true)
            )

            // Validation Error Alert Text
            if (validationState is ValidationResult.Invalid) {
                Text(
                    text = (validationState as ValidationResult.Invalid).reason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingExtraSmall)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingExtraSmall))

            // Color Picker Indicator
            Text("Select Color", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                PredefinedColors.take(5).forEach { colorInt ->
                    ColorCircle(
                        color = Color(colorInt),
                        isSelected = selectedColor == colorInt,
                        onClick = { selectedColor = colorInt }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                PredefinedColors.drop(5).forEach { colorInt ->
                    ColorCircle(
                        color = Color(colorInt),
                        isSelected = selectedColor == colorInt,
                        onClick = { selectedColor = colorInt }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Required Attendance Percentage Selector
            Text(
                text = "Required Attendance: $requiredAttendance%",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = requiredAttendance.toFloat(),
                onValueChange = { requiredAttendance = it.toInt() },
                valueRange = 1f..100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Personal Target Percentage Selector
            Text(
                text = "Personal Attendance Goal: $personalGoal%",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = personalGoal.toFloat(),
                onValueChange = { personalGoal = it.toInt() },
                valueRange = requiredAttendance.toFloat()..100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Actions Buttons
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
                        coroutineScope.launch {
                            val success = viewModel.saveSubject(
                                id = if (subjectId == -1L) 0L else subjectId,
                                name = name,
                                faculty = faculty,
                                color = selectedColor,
                                required = requiredAttendance,
                                goal = personalGoal
                            )
                            if (success) {
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

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
