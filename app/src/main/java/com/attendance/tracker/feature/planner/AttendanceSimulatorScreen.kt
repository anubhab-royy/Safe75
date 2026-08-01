package com.attendance.tracker.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.dashboard.DashboardViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSimulatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val simPresent by viewModel.simPresentInput.collectAsState()
    val simAbsent by viewModel.simAbsentInput.collectAsState()
    val result by viewModel.simulationResult.collectAsState()

    var presentStr by remember { mutableStateOf(simPresent.toString().takeIf { it != "0" } ?: "") }
    var absentStr by remember { mutableStateOf(simAbsent.toString().takeIf { it != "0" } ?: "") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Attendance Simulator",
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
            // Simulator explanation banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                    Text(
                        text = "Hypothetical Scenario Planner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estimate how attending or bunking upcoming classes impacts your overall percentage. This will not change your database logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Input Fields
            Text(
                text = "Simulator Parameters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                OutlinedTextField(
                    value = presentStr,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            presentStr = input
                            val count = input.toIntOrNull() ?: 0
                            viewModel.onSimPresentChange(count)
                        }
                    },
                    label = { Text("Attend Next X") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = absentStr,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            absentStr = input
                            val count = input.toIntOrNull() ?: 0
                            viewModel.onSimAbsentChange(count)
                        }
                    },
                    label = { Text("Bunk Next Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Result Display Card
            result?.let { simResult ->
                Text(
                    text = "Projected Results",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SimStatsColumn(
                                label = "Current Pct",
                                pct = simResult.currentPercentage
                            )
                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(50.dp)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            )
                            SimStatsColumn(
                                label = "Projected Pct",
                                pct = simResult.simulatedPercentage
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

                        val diffColor = when {
                            simResult.difference > 0.0 -> Color(0xFF2E7D32)
                            simResult.difference < 0.0 -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }

                        val prefix = if (simResult.difference > 0.0) "+" else ""

                        Text(
                            text = String.format(Locale.getDefault(), "Difference: %s%.2f%%", prefix, simResult.difference),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = diffColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimStatsColumn(label: String, pct: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format(Locale.getDefault(), "%.2f%%", pct),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}
