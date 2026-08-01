package com.attendance.tracker.feature.archive

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.components.EmptyState
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.ArchiveData
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

/**
 * Archive browser listing all archived semesters with their summary and
 * attendance percentage. Tapping an entry opens read-only [ArchiveDetailsScreen].
 * A FAB opens the "Archive Semester" creation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val archives by viewModel.archives.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Semester Archives",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "Archive current semester")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (archives.isEmpty()) {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    message = "No archived semesters",
                    description = "Use the + button to archive the current semester."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimensions.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    items(archives, key = { it.id }) { archive ->
                        ArchiveCard(
                            archive = archive,
                            onClick = { onNavigateToDetails(archive.id) },
                            onDelete = { viewModel.requestDelete(archive.id) }
                        )
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateArchiveDialog(
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = { name, startDate, endDate ->
                viewModel.createArchive(name, startDate, endDate)
            }
        )
    }

    state.showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete Archive") },
            text = { Text("This permanently removes the archived semester. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteArchive(id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ArchiveCard(
    archive: ArchiveData,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(Dimensions.SpacingMedium))
            Column(Modifier.weight(1f)) {
                Text(
                    archive.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${archive.startDate} → ${archive.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${archive.totalClasses} classes • " +
                    "${archive.presentCount} present • ${archive.absentCount} absent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.ROOT, "%.1f%%", archive.overallPercentage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = attendanceColor(archive.overallPercentage)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete archive",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateArchiveDialog(
    onDismiss: () -> Unit,
    onCreate: (String, LocalDate, LocalDate) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(120)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val calendar = Calendar.getInstance()

    fun showDatePicker(initial: LocalDate, onSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive Current Semester") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                Text(
                    "This captures the current subjects, schedules, and attendance as a " +
                    "read-only snapshot that stays valid even after a semester reset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name") },
                    placeholder = { Text("e.g. Fall 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DateField(
                    label = "Start Date",
                    value = startDate,
                    onClick = { showDatePicker(startDate) { startDate = it } }
                )
                DateField(
                    label = "End Date",
                    value = endDate,
                    onClick = { showDatePicker(endDate) { endDate = it } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim().ifBlank { "Semester ${endDate.year}" }, startDate, endDate) },
                enabled = name.isNotBlank() && !endDate.isBefore(startDate)
            ) { Text("Archive") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DateField(
    label: String,
    value: LocalDate,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimensions.SpacingSmall))
        Text("$label: $value")
    }
}

private fun attendanceColor(percentage: Double) = when {
    percentage >= 75.0 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    percentage >= 60.0 -> androidx.compose.ui.graphics.Color(0xFFF9A825)
    else -> androidx.compose.ui.graphics.Color(0xFFC62828)
}
