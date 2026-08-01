package com.attendance.tracker.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.domain.model.RestoreResult

/**
 * Dedicated restore flow screen.
 *
 * 1. The user selects a JSON backup file via the Storage Access Framework.
 * 2. The file is parsed and validated (no data is written yet).
 * 3. A [BackupPreviewDialog] shows the contents.
 * 4. The user picks restore categories in [RestoreOptionsDialog].
 * 5. The restore is committed only after confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedImportUri = it
            viewModel.previewBackup(it)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.restoreResult) {
        when (val r = state.restoreResult) {
            is RestoreResult.Success -> {
                snackbarHostState.showSnackbar(
                    "Restored ${r.subjectsRestored} subjects, ${r.schedulesRestored} schedules, " +
                    "${r.attendanceRestored} attendance records."
                )
                viewModel.clearResults()
            }
            is RestoreResult.Failure -> {
                snackbarHostState.showSnackbar("Restore failed: ${r.error}")
                viewModel.clearResults()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Restore from Backup",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Dimensions.SpacingSmall)
                    )
                    Text(
                        "Select a Backup File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(Dimensions.SpacingSmall))
                    Text(
                        "The file will be validated and shown in a preview. " +
                        "Nothing is changed until you confirm the restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Dimensions.SpacingMedium))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoading) "Validating..." else "Select JSON File")
                    }
                }
            }

            if (state.validationErrors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SpacingMedium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(Dimensions.SpacingSmall))
                            Text(
                                "Validation Failed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        state.validationErrors.forEach { error ->
                            Spacer(Modifier.height(4.dp))
                            Text("• $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }

    // 1. Preview dialog (shown after successful validation, before restore options).
    val preview = state.previewData
    if (preview != null && !state.showRestoreOptions) {
        BackupPreviewDialog(
            previewData = preview,
            onDismiss = { viewModel.dismissRestoreDialog() },
            onContinue = { viewModel.updateRestoreOptions(state.restoreOptions); viewModel.goToRestoreOptions() }
        )
    }

    // 2. Restore options dialog.
    if (state.showRestoreOptions && preview != null && selectedImportUri != null) {
        RestoreOptionsDialog(
            previewData = preview,
            options = state.restoreOptions,
            onOptionsChanged = viewModel::updateRestoreOptions,
            onConfirm = { viewModel.importBackup(selectedImportUri!!, state.restoreOptions) },
            onDismiss = viewModel::dismissRestoreDialog
        )
    }
}
