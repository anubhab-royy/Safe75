package com.attendance.tracker.feature.bugreport

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.tracker.core.ui.components.AppTopBar
import com.attendance.tracker.core.ui.theme.Dimensions
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment

@Composable
fun BugReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: BugReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onScreenshotSelected(it.toString()) } }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Report a problem",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        when (val current = state) {
            is BugReportUiState.Success -> SuccessContent(
                onNewReport = viewModel::startNewReport,
                modifier = Modifier.padding(paddingValues)
            )
            else -> {
                val draft = state.draft()
                val busy = state.isBusy()
                BugReportForm(
                    description = draft.description,
                    screenshot = draft.screenshot,
                    errorMessage = state.errorMessage(),
                    busy = busy,
                    progressMessage = state.progressMessage(),
                    onDescriptionChanged = viewModel::onDescriptionChanged,
                    onPickScreenshot = { picker.launch(arrayOf("image/*")) },
                    onRemoveScreenshot = viewModel::removeScreenshot,
                    onSubmit = viewModel::submit,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun BugReportForm(
    description: String,
    screenshot: ScreenshotAttachment?,
    errorMessage: String?,
    busy: Boolean,
    progressMessage: String?,
    onDescriptionChanged: (String) -> Unit,
    onPickScreenshot: () -> Unit,
    onRemoveScreenshot: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
    ) {
        Text(
            text = "Help us understand what happened",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Describe the issue and optionally attach one screenshot. Diagnostics are generated only when you submit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            label = { Text("What went wrong?") },
            placeholder = { Text("Tell us what you expected and what happened") },
            minLines = 5,
            maxLines = 8,
            supportingText = {
                Text(
                    text = "${description.length}/1000",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        )

        if (screenshot == null) {
            OutlinedButton(
                onClick = onPickScreenshot,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(Modifier.width(Dimensions.SpacingSmall))
                Text("Attach screenshot")
            }
        } else {
            ScreenshotPreview(
                screenshot = screenshot,
                enabled = !busy,
                onRemove = onRemoveScreenshot
            )
        }

        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = progressMessage ?: "Submitting report...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(Dimensions.SpacingSmall))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.ButtonHeight)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(Dimensions.SpacingSmall))
                Text("Submit report")
            }
        }
    }
}

@Composable
private fun ScreenshotPreview(
    screenshot: ScreenshotAttachment,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val bitmap = remember(screenshot.uri) {
        BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)?.asImageBitmap()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Selected screenshot",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimensions.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screenshot attached",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove screenshot")
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    onNewReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(Dimensions.SpacingMedium))
        Text("Report saved", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Dimensions.SpacingSmall))
        Text(
            text = "Your report has been saved locally and queued for upload. It will be sent automatically as soon as it can be delivered.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimensions.SpacingLarge))
        Button(onClick = onNewReport) { Text("Submit another report") }
    }
}

private data class DraftUi(
    val description: String,
    val screenshot: ScreenshotAttachment?
)

private fun BugReportUiState.draft(): DraftUi = when (this) {
    BugReportUiState.Idle -> DraftUi("", null)
    is BugReportUiState.Editing -> DraftUi(description, screenshot)
    is BugReportUiState.Submitting -> DraftUi(description, screenshot)
    is BugReportUiState.UploadingScreenshot -> DraftUi(description, screenshot)
    is BugReportUiState.Failure -> DraftUi(description, screenshot)
    is BugReportUiState.ValidationError -> DraftUi(description, screenshot)
    is BugReportUiState.Success -> DraftUi("", null)
}

private fun BugReportUiState.isBusy(): Boolean =
    this is BugReportUiState.Submitting || this is BugReportUiState.UploadingScreenshot

private fun BugReportUiState.errorMessage(): String? = when (this) {
    is BugReportUiState.Failure -> message
    is BugReportUiState.ValidationError -> message
    else -> null
}

private fun BugReportUiState.progressMessage(): String? = when (this) {
    is BugReportUiState.Submitting -> "Submitting report..."
    is BugReportUiState.UploadingScreenshot -> "Uploading screenshot..."
    else -> null
}
