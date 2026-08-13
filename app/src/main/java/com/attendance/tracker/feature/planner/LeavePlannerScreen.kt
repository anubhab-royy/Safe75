package com.attendance.tracker.feature.planner

import com.attendance.tracker.R
import androidx.compose.ui.res.stringResource
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.dashboard.DashboardViewModel
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LeavePlannerScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val leaveDates by viewModel.selectedLeaveDates.collectAsStateWithLifecycle()
    val plannerResult by viewModel.leavePlannerResult.collectAsStateWithLifecycle()

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.toggleLeaveDate(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Leave Planner",
                showBackButton = true,
                onBackClick = onNavigateBack
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
            // Explain planner banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                    Text(
                        text = "Smart Leave Assessment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estimate projected drops before applying for holidays. It maps dates against schedules to calculate the simulated outcome.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Input Selection Button controls
            Text(
                text = "Planned Absence Dates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                Button(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                    Text(stringResource(R.string.select_date))
                }

                if (leaveDates.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearLeaveDates() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                        Text(stringResource(R.string.clear_all))
                    }
                }
            }

            // Selected Dates chips flow
            if (leaveDates.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    leaveDates.sorted().forEach { date ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleLeaveDate(date) },
                            label = { Text(date.toString()) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove date",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Leave Projection results cards
            if (leaveDates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SpacingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No dates selected",
                        description = "Add dates above to analyze projected attendance levels."
                    )
                }
            } else {
                plannerResult?.let { result ->
                    Text(
                        text = "Leave Assessment Result",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    // Overall projected drop card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isOverallSafe) {
                                Color(0xFFE8F5E9) // soft green
                            } else {
                                Color(0xFFFFEBEE) // soft red
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (result.isOverallSafe) "SAFE TO LEAVE" else "UNSAFE TO LEAVE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isOverallSafe) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (result.isOverallSafe) Color(0xFF2E7D32).copy(alpha = 0.1f)
                                            else Color(0xFFC62828).copy(alpha = 0.1f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (result.isOverallSafe) "Recommendation: OK" else "Recommendation: Warning",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (result.isOverallSafe) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "Projected overall percentage drops from %.1f%% to %.1f%%",
                                    result.currentOverallPercentage,
                                    result.projectedOverallPercentage
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }

                    // Affected subjects listing
                    Text(
                        text = "Affected Subjects Detail",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    result.affectedSubjects.forEach { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(sub.subjectColor))
                                )
                                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.subjectName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Projected Missed Classes: ${sub.missedClassesCount}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "Projected drop: %.1f%% -> %.1f%%",
                                            sub.currentPercentage,
                                            sub.projectedPercentage
                                        ),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                Text(
                                    text = if (sub.isProjectedSafe) "SAFE" else "UNSAFE",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.isProjectedSafe) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
