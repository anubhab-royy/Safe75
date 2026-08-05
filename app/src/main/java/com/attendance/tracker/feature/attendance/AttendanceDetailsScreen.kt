package com.attendance.tracker.feature.attendance

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.LoadingIndicator
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.launch

/**
 * Screen providing controls to edit an existing attendance log status,
 * remarks, or delete it permanently from the Room database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailsScreen(
    attendanceId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val subjectsMap by viewModel.subjectsMap.collectAsStateWithLifecycle()
    val validationState by viewModel.validationState.collectAsStateWithLifecycle()

    var record by remember { mutableStateOf<Attendance?>(null) }
    var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var remarks by remember { mutableStateOf("") }

    var isLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(attendanceId) {
        if (!isLoaded) {
            val fetched = viewModel.getAttendanceById(attendanceId)
            if (fetched != null) {
                record = fetched
                status = fetched.status
                remarks = fetched.remarks ?: ""
            }
            isLoaded = true
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_attendance_log)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        record?.let {
                            viewModel.deleteAttendance(
                                id = it.id,
                                subjectId = it.subjectId,
                                scheduleId = it.scheduleId,
                                date = it.date,
                                status = it.status,
                                remarks = it.remarks
                            )
                        }
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Edit Attendance",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (record == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (isLoaded) {
                    Text(stringResource(R.string.attendance_log_not_found))
                } else {
                    LoadingIndicator()
                }
            }
        } else {
            val targetRecord = record!!
            val subjectName = subjectsMap[targetRecord.subjectId]?.name ?: "Unknown Subject"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(Dimensions.SpacingMedium)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                // Class Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Date: ${targetRecord.date}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Change status selector radio buttons
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AttendanceStatusRow(
                        label = "Present (Green)",
                        isSelected = status == AttendanceStatus.PRESENT,
                        onClick = { status = AttendanceStatus.PRESENT }
                    )
                    AttendanceStatusRow(
                        label = "Absent (Red)",
                        isSelected = status == AttendanceStatus.ABSENT,
                        onClick = { status = AttendanceStatus.ABSENT }
                    )
                    AttendanceStatusRow(
                        label = "Cancelled (Gray)",
                        isSelected = status == AttendanceStatus.CANCELLED,
                        onClick = { status = AttendanceStatus.CANCELLED }
                    )
                }

                // Remarks TextField
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(stringResource(R.string.remarks_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationState is ValidationResult.Invalid) {
                    Text(
                        text = (validationState as ValidationResult.Invalid).reason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = viewModel.updateAttendance(
                                    id = targetRecord.id,
                                    subjectId = targetRecord.subjectId,
                                    scheduleId = targetRecord.scheduleId,
                                    date = targetRecord.date,
                                    status = status,
                                    remarks = remarks
                                )
                                if (success) {
                                    viewModel.clearValidationError()
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingLarge))

                // Danger section delete buttons
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delete_record))
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatusRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
