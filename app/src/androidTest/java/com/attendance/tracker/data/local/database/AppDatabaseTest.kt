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

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        subjectDao = db.subjectDao()
        scheduleDao = db.scheduleDao()
        attendanceDao = db.attendanceDao()
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
}
