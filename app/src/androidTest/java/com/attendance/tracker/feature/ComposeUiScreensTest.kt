package com.attendance.tracker.feature

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.attendance.tracker.feature.dashboard.DashboardScreen
import com.attendance.tracker.feature.dashboard.DashboardViewModel
import com.attendance.tracker.feature.subject.SubjectsScreen
import com.attendance.tracker.feature.subject.SubjectViewModel
import com.attendance.tracker.feature.attendance.AttendanceHistoryScreen
import com.attendance.tracker.feature.attendance.AttendanceViewModel
import com.attendance.tracker.feature.ocr.OCRReviewScreen
import com.attendance.tracker.feature.ocr.review.OcrReviewViewModel
import com.attendance.tracker.feature.backup.BackupScreen
import com.attendance.tracker.feature.backup.BackupViewModel
import com.attendance.tracker.feature.backup.BackupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ComposeUiScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardScreen_displaysDashboardText() {
        val viewModel = mock(DashboardViewModel::class.java)
        `when`(viewModel.activeVersion).thenReturn(MutableStateFlow(null))
        `when`(viewModel.dashboardStats).thenReturn(MutableStateFlow(null))
        `when`(viewModel.subjectStatsList).thenReturn(MutableStateFlow(emptyList()))
        `when`(viewModel.todayClasses).thenReturn(MutableStateFlow(emptyList()))
        `when`(viewModel.attendanceGoal).thenReturn(MutableStateFlow(75.0))

        composeTestRule.setContent {
            DashboardScreen(
                onNavigateToAttendanceHistory = {},
                onNavigateToSimulator = {},
                onNavigateToLeavePlanner = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Overall Stats").assertIsDisplayed()
    }

    @Test
    fun testSubjectsScreen_displaysSubjectsTitle() {
        val viewModel = mock(SubjectViewModel::class.java)
        `when`(viewModel.subjectsState).thenReturn(MutableStateFlow(com.attendance.tracker.core.common.UiState.Success(emptyList())))
        `when`(viewModel.searchQuery).thenReturn(MutableStateFlow(""))
        `when`(viewModel.sortOption).thenReturn(MutableStateFlow(com.attendance.tracker.feature.subject.SubjectSortOption.ALPHABETICAL))

        composeTestRule.setContent {
            SubjectsScreen(
                onNavigateToAddEditSubject = {},
                onNavigateToBackfill = {},
                viewModel = viewModel
            )
        }
        composeTestRule.onNodeWithText("Subjects").assertIsDisplayed()
    }

    @Test
    fun testAttendanceHistoryScreen_displaysHistoryTitle() {
        val viewModel = mock(AttendanceViewModel::class.java)
        `when`(viewModel.searchQuery).thenReturn(MutableStateFlow(""))
        `when`(viewModel.selectedSubjectId).thenReturn(MutableStateFlow(null))
        `when`(viewModel.selectedDate).thenReturn(MutableStateFlow(null))
        `when`(viewModel.subjectsMap).thenReturn(MutableStateFlow(emptyMap()))
        `when`(viewModel.historyRecords).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            AttendanceHistoryScreen(
                onNavigateBack = {},
                onNavigateToAttendanceDetails = {},
                viewModel = viewModel
            )
        }
        composeTestRule.onNodeWithText("Attendance History").assertIsDisplayed()
    }

    @Test
    fun testOCRReviewScreen_displaysReviewTitle() {
        val viewModel = mock(OcrReviewViewModel::class.java)
        `when`(viewModel.timetableRows).thenReturn(MutableStateFlow(emptyList()))
        `when`(viewModel.attendanceRows).thenReturn(MutableStateFlow(emptyList()))
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false))
        `when`(viewModel.error).thenReturn(MutableStateFlow(null))
        `when`(viewModel.subjects).thenReturn(MutableStateFlow(emptyList()))

        composeTestRule.setContent {
            OCRReviewScreen(
                isTimetable = true,
                imageUri = Uri.EMPTY,
                onNavigateBack = {},
                viewModel = viewModel
            )
        }
        composeTestRule.onNodeWithText("Review Extracted Data").assertIsDisplayed()
    }

    @Test
    fun testBackupScreen_displaysBackupTitle() {
        val viewModel = mock(BackupViewModel::class.java)
        `when`(viewModel.uiState).thenReturn(MutableStateFlow(BackupUiState()))

        composeTestRule.setContent {
            BackupScreen(
                onNavigateBack = {},
                onNavigateToRestore = {},
                onNavigateToIntegrity = {},
                onNavigateToSemesterReset = {},
                viewModel = viewModel
            )
        }
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
    }
}
