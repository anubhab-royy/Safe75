package com.attendance.tracker.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.BugReportQueueEntity
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var subjectDao: SubjectDao
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var attendanceDao: AttendanceDao
    private lateinit var semesterDao: com.attendance.tracker.data.local.database.dao.SemesterDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        subjectDao = db.subjectDao()
        scheduleDao = db.scheduleDao()
        attendanceDao = db.attendanceDao()
        semesterDao = db.semesterDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeSubjectAndReadInList() = runBlocking {
        val subject = SubjectEntity(
            id = 1L,
            name = "Mathematics",
            facultyName = "Dr. Euler",
            color = 0xFF0000,
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        subjectDao.upsertSubject(subject)
        val subjects = subjectDao.getSubjects()
        assertEquals(1, subjects.size)
        assertEquals("Mathematics", subjects[0].name)
    }

    @Test
    fun writeScheduleAndRead() = runBlocking {
        val subject = SubjectEntity(
            id = 1L,
            name = "Mathematics",
            facultyName = "Dr. Euler",
            color = 0xFF0000,
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        subjectDao.upsertSubject(subject)

        val schedule = ScheduleEntity(
            id = 10L,
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = "Room 303",
            teacherOverride = null,
            versionId = 1L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        scheduleDao.upsertSchedule(schedule)
        val list = scheduleDao.getSchedulesForVersion(1L)
        assertEquals(1, list.size)
        assertEquals("Room 303", list[0].room)
    }

    @Test
    fun writeBugReportQueueAndRead() = runBlocking {
        val entity = BugReportQueueEntity(
            reportId = "local-report-1",
            fingerprint = "fingerprint-1",
            timestamp = 1L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "15",
            sdkVersion = 35,
            deviceManufacturer = "Google",
            deviceModel = "Pixel",
            cpuAbi = "arm64-v8a",
            locale = "en-US",
            crashReportId = null,
            userDescription = "The dashboard is blank",
            diagnosticsMetadata = "{}",
            screenshotPath = null,
            screenshotContentType = null,
            remoteReportId = null,
            status = "QUEUED",
            retryCount = 0,
            createdAt = 1L,
            updatedAt = 1L,
            lastError = null
        )

        db.bugReportQueueDao().insert(entity)

        val loaded = db.bugReportQueueDao().findById("local-report-1")
        assertNotNull(loaded)
        assertEquals("The dashboard is blank", loaded?.userDescription)
        assertEquals("QUEUED", loaded?.status)
    }

    @Test
    fun medicalLeaveRoundTripsAndUpdatesExistingAttendanceRow() = runBlocking {
        subjectDao.upsertSubject(
            SubjectEntity(
                id = 1L,
                name = "Mathematics",
                requiredAttendancePercentage = 75,
                personalAttendanceGoal = 85,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        semesterDao.insertVersion(
            SemesterVersionEntity(
                id = 1L,
                name = "Semester 1",
                isActive = true,
                createdAt = 1L,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 6, 1)
            )
        )
        scheduleDao.upsertSchedule(
            ScheduleEntity(
                id = 10L,
                subjectId = 1L,
                dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                versionId = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val date = LocalDate.of(2026, 2, 2)
        val id = attendanceDao.insert(
            AttendanceEntity(
                subjectId = 1L,
                scheduleId = 10L,
                date = date,
                status = AttendanceStatus.MEDICAL_LEAVE,
                createdAt = 123L,
                updatedAt = 123L
            )
        )

        val loaded = attendanceDao.getAttendanceById(id)
        assertNotNull(loaded)
        assertEquals(AttendanceStatus.MEDICAL_LEAVE, loaded?.status)

        val rowsUpdated = attendanceDao.update(
            loaded!!.copy(status = AttendanceStatus.PRESENT, updatedAt = 456L)
        )
        assertEquals(1, rowsUpdated)
        assertEquals(1, attendanceDao.getAllAttendance().size)
        assertEquals(AttendanceStatus.PRESENT, attendanceDao.getAttendanceById(id)?.status)
        assertEquals(123L, attendanceDao.getAttendanceById(id)?.createdAt)
    }
}
