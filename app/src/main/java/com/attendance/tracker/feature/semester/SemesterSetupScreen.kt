package com.attendance.tracker.feature.semester

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import com.attendance.tracker.domain.model.Schedule as DomainSchedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.ui.theme.Dimensions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSetupScreen(
    onNavigateToOcr: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SemesterSetupViewModel = hiltViewModel()
) {
    val step by viewModel.currentStep.collectAsState()
    val name by viewModel.semesterName.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val goal by viewModel.attendanceGoal.collectAsState()
    val schedules by viewModel.schedulesList.collectAsState()
    val pastClasses by viewModel.pastClassesList.collectAsState()

    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Semester Setup Wizard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (step != SetupStep.DETAILS) {
                        IconButton(onClick = {
                            val prevStep = when (step) {
                                SetupStep.TIMETABLE -> SetupStep.DETAILS
                                SetupStep.REVIEW_SCHEDULE -> SetupStep.TIMETABLE
                                SetupStep.BACKFILL -> SetupStep.REVIEW_SCHEDULE
                                else -> SetupStep.DETAILS
                            }
                            viewModel.setStep(prevStep)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Step Indicator Header
            StepIndicator(currentStep = step)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    SetupStep.DETAILS -> {
                        DetailsStepView(
                            name = name,
                            startDate = startDate,
                            goal = goal,
                            dateFormatter = dateFormatter,
                            onNameChange = viewModel::setSemesterName,
                            onStartDateClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val date = LocalDate.of(year, month + 1, dayOfMonth)
                                        viewModel.setStartDate(date)
                                    },
                                    startDate.year,
                                    startDate.monthValue - 1,
                                    startDate.dayOfMonth
                                ).apply {
                                    datePicker.maxDate = System.currentTimeMillis()
                                    show()
                                }
                            },
                            onGoalChange = viewModel::setAttendanceGoal,
                            onNext = { viewModel.createSemester() }
                        )
                    }
                    SetupStep.TIMETABLE -> {
                        TimetableStepView(
                            hasClasses = schedules.isNotEmpty(),
                            onNavigateToOcr = onNavigateToOcr,
                            onManualSetup = { viewModel.setStep(SetupStep.REVIEW_SCHEDULE) }
                        )
                    }
                    SetupStep.REVIEW_SCHEDULE -> {
                        ReviewScheduleStepView(
                            schedules = schedules,
                            onNext = { viewModel.setStep(SetupStep.BACKFILL) }
                        )
                    }
                    SetupStep.BACKFILL -> {
                        BackfillStepView(
                            pastClasses = pastClasses,
                            startDate = startDate,
                            dateFormatter = dateFormatter,
                            onMarkAttendance = viewModel::markPastAttendance,
                            onFinish = onNavigateToDashboard
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: SetupStep) {
    val steps = listOf("Details", "Timetable", "Review", "Backfill")
    val currentIndex = currentStep.ordinal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentIndex
            val isCompleted = index < currentIndex
            val color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(
                            width = 1.5.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = fontWeight,
                    color = color
                )
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "→",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsStepView(
    name: String,
    startDate: LocalDate,
    goal: Int,
    dateFormatter: DateTimeFormatter,
    onNameChange: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onGoalChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "Semester Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter your current semester parameters. The start date helps generate historical schedule logs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Semester Name
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Semester Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Start Date Selector
            OutlinedTextField(
                value = startDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Semester Start Date") },
                trailingIcon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        modifier = Modifier.clickable { onStartDateClick() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartDateClick() }
            )

            // Attendance Goal Target
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Attendance Goal Target", style = MaterialTheme.typography.bodyMedium)
                    Text("$goal%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { onGoalChange(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 50
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Timetable", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimetableStepView(
    hasClasses: Boolean,
    onNavigateToOcr: () -> Unit,
    onManualSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "Timetable Import",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add class slots so we can calculate dates when each subject is held.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToOcr() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Import Timetable from Image (OCR)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Scan your screenshot using offline text recognition.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManualSetup() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Setup Manually / Skip for Now", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Enter class slots manually in the schedule editor.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Button(
            onClick = onManualSetup,
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (hasClasses) "Continue (Classes found)" else "Skip / Manual Setup",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReviewScheduleStepView(
    schedules: List<DomainSchedule>,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Review Timetable",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (schedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No class slots added yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedules) { schedule ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(schedule.dayOfWeek.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    Text("${schedule.startTime} - ${schedule.endTime}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (schedule.room != null) {
                                        Text("Room: ${schedule.room}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Next: Backfill Attendance", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackfillStepView(
    pastClasses: List<PastClassItem>,
    startDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    onMarkAttendance: (PastClassItem, AttendanceStatus) -> Unit,
    onFinish: () -> Unit
) {
    val today = LocalDate.now()
    val hasPastClasses = pastClasses.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Backfill Past Classes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Since the semester started in the past (${startDate.format(dateFormatter)}), you can log attendance for past days to ensure calculations remain 100% correct.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasPastClasses) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No past scheduled classes to backfill.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val grouped = pastClasses.groupBy { it.date }
                    grouped.forEach { (date, classes) ->
                        item {
                            Text(
                                text = date.format(dateFormatter),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        items(classes) { item ->
                            PastClassRow(item = item, onMarkAttendance = onMarkAttendance)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Finish & Open Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PastClassRow(
    item: PastClassItem,
    onMarkAttendance: (PastClassItem, AttendanceStatus) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(item.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${item.startTime} - ${item.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Log Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Present Button
                val isP = item.status == AttendanceStatus.PRESENT
                Button(
                    onClick = { onMarkAttendance(item, AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isP) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("P", fontWeight = FontWeight.Bold)
                }

                // Absent Button
                val isA = item.status == AttendanceStatus.ABSENT
                Button(
                    onClick = { onMarkAttendance(item, AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isA) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isA) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("A", fontWeight = FontWeight.Bold)
                }

                // Cancelled Button
                val isC = item.status == AttendanceStatus.CANCELLED
                Button(
                    onClick = { onMarkAttendance(item, AttendanceStatus.CANCELLED) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isC) Color(0xFF757575) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isC) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("C", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
