package com.attendance.tracker.feature.ocr

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.attendance.tracker.domain.model.Subject
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.LoadingIndicator
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.review.OcrReviewViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRReviewScreen(
    isTimetable: Boolean,
    imageUri: Uri,
    onNavigateBack: () -> Unit,
    viewModel: OcrReviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val timetableRows by viewModel.timetableRows.collectAsStateWithLifecycle()
    val attendanceRows by viewModel.attendanceRows.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    // Holds subject ID mapping updates for attendance screenshot mapping dialogs
    val subjectMappings = remember { mutableStateMapOf<String, Long>() }

    LaunchedEffect(imageUri) {
        if (isTimetable) {
            viewModel.scanTimetable(context, imageUri)
        } else {
            viewModel.scanAttendance(context, imageUri)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Review Extracted Data",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                    Text(stringResource(R.string.processing_image_text_recognition))
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(Dimensions.SpacingMedium),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    Text(text = "OCR Failed: $error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        if (isTimetable) {
                            viewModel.scanTimetable(context, imageUri)
                        } else {
                            viewModel.scanAttendance(context, imageUri)
                        }
                    }) {
                        Text(stringResource(R.string.retry_scanning))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                val isEmpty = if (isTimetable) timetableRows.isEmpty() else attendanceRows.isEmpty()

                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                        ) {
                            Text(stringResource(R.string.no_structures_detected))
                            OutlinedButton(onClick = onNavigateBack) {
                                Text(stringResource(R.string.go_back))
                            }
                        }
                    }
                } else {
                    var showUncertainOnly by remember { mutableStateOf(true) }

                    val filteredTimetableRows = remember(timetableRows, showUncertainOnly) {
                        if (showUncertainOnly) {
                            timetableRows.filter { row ->
                                row.subjectName.confidence < 0.85f ||
                                row.dayOfWeek.confidence < 0.85f ||
                                row.startTime.confidence < 0.85f ||
                                row.endTime.confidence < 0.85f
                            }
                        } else {
                            timetableRows
                        }
                    }

                    val filteredAttendanceRows = remember(attendanceRows, showUncertainOnly) {
                        if (showUncertainOnly) {
                            attendanceRows.filter { row ->
                                row.subjectName.confidence < 0.85f ||
                                row.percentage.confidence < 0.85f
                            }
                        } else {
                            attendanceRows
                        }
                    }

                    val showEmptyUncertain = if (isTimetable) filteredTimetableRows.isEmpty() else filteredAttendanceRows.isEmpty()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showUncertainOnly) "Showing Uncertain Entries Only" else "Showing All Scanned Rows",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Uncertain Only",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = Dimensions.SpacingSmall)
                            )
                            androidx.compose.material3.Switch(
                                checked = showUncertainOnly,
                                onCheckedChange = { showUncertainOnly = it }
                            )
                        }
                    }

                    if (showEmptyUncertain) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(Dimensions.SpacingMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                            ) {
                                Text(
                                    text = "All parsed items have high confidence!",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Disable the filter toggle above to review everything.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(Dimensions.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                        ) {
                            if (isTimetable) {
                                items(filteredTimetableRows, key = { it.id }) { row ->
                                    TimetableReviewCard(
                                        row = row,
                                        onEdit = { viewModel.updateTimetableRow(it) },
                                        onDelete = { viewModel.deleteTimetableRow(row.id) }
                                    )
                                }
                            } else {
                                items(filteredAttendanceRows, key = { it.id }) { row ->
                                    AttendanceReviewCard(
                                        row = row,
                                        subjects = subjects,
                                        currentMapping = subjectMappings[row.id] ?: row.matchedSubjectId,
                                        onEdit = { viewModel.updateAttendanceRow(it) },
                                        onDelete = { viewModel.deleteAttendanceRow(row.id) },
                                        onMap = { subjectMappings[row.id] = it }
                                    )
                                }
                            }
                        }
                    }

                    // Save options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.SpacingMedium),
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
                                    val success = if (isTimetable) {
                                        viewModel.saveTimetable()
                                    } else {
                                        viewModel.saveAttendance(subjectMappings)
                                    }
                                    if (success) {
                                        onNavigateBack()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_all))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.90f -> Color(0xFF2E7D32)
        confidence >= 0.70f -> Color(0xFFFFA000)
        else -> Color(0xFFC62828)
    }
}

@Composable
private fun TimetableReviewCard(
    row: OcrTimetableRow,
    onEdit: (OcrTimetableRow) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Confidence badge indicator
                val confidencePct = (row.subjectName.confidence * 100).toInt()
                val color = getConfidenceColor(row.subjectName.confidence)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Confidence: $confidencePct%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete row",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Editable Input rows
            OutlinedTextField(
                value = row.subjectName.value,
                onValueChange = { onEdit(row.copy(subjectName = OcrField(it, row.subjectName.confidence))) },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                OutlinedTextField(
                    value = row.dayOfWeek.value,
                    onValueChange = { onEdit(row.copy(dayOfWeek = OcrField(it, row.dayOfWeek.confidence))) },
                    label = { Text(stringResource(R.string.day)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = row.startTime.value,
                    onValueChange = { onEdit(row.copy(startTime = OcrField(it, row.startTime.confidence))) },
                    label = { Text(stringResource(R.string.start_time)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = row.endTime.value,
                    onValueChange = { onEdit(row.copy(endTime = OcrField(it, row.endTime.confidence))) },
                    label = { Text(stringResource(R.string.end_time)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                OutlinedTextField(
                    value = row.faculty.value ?: "",
                    onValueChange = { onEdit(row.copy(faculty = OcrField(it.takeIf { it.isNotBlank() }, row.faculty.confidence))) },
                    label = { Text(stringResource(R.string.teacher_opt)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = row.room.value ?: "",
                    onValueChange = { onEdit(row.copy(room = OcrField(it.takeIf { it.isNotBlank() }, row.room.confidence))) },
                    label = { Text(stringResource(R.string.room_opt)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceReviewCard(
    row: OcrAttendanceRow,
    subjects: List<Subject>,
    currentMapping: Long?,
    onEdit: (OcrAttendanceRow) -> Unit,
    onDelete: () -> Unit,
    onMap: (Long) -> Unit
) {
    var showMapDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val confidencePct = (row.subjectName.confidence * 100).toInt()
                val color = getConfidenceColor(row.subjectName.confidence)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Confidence: $confidencePct%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete row",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedTextField(
                value = row.subjectName.value,
                onValueChange = { onEdit(row.copy(subjectName = OcrField(it, row.subjectName.confidence))) },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                OutlinedTextField(
                    value = row.presentCount.value.toString(),
                    onValueChange = { input ->
                        val count = input.toIntOrNull() ?: 0
                        onEdit(row.copy(presentCount = OcrField(count, row.presentCount.confidence)))
                    },
                    label = { Text(stringResource(R.string.present)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = row.totalClasses.value.toString(),
                    onValueChange = { input ->
                        val count = input.toIntOrNull() ?: 0
                        onEdit(row.copy(totalClasses = OcrField(count, row.totalClasses.confidence)))
                    },
                    label = { Text(stringResource(R.string.total)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%.1f", row.percentage.value),
                    onValueChange = { input ->
                        val pct = input.toDoubleOrNull() ?: 0.0
                        onEdit(row.copy(percentage = OcrField(pct, row.percentage.confidence)))
                    },
                    label = { Text(stringResource(R.string.percentage)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subject mapping selector
            Text(
                text = "Database Mapping",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                val activeMappedName = subjects.find { it.id == currentMapping }?.name ?: "Tap to Map to existing Subject..."
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showMapDropdown = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = activeMappedName, fontSize = 14.sp)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showMapDropdown,
                    onDismissRequest = { showMapDropdown = false }
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                onMap(subject.id)
                                showMapDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}
