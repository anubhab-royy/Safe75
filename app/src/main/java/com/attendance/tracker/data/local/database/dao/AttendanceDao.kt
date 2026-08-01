package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object defining queries for attendance logs.
 */
@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attendance: AttendanceEntity): Long

    @Update
    suspend fun update(attendance: AttendanceEntity): Int

    @Delete
    suspend fun delete(attendance: AttendanceEntity): Int

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    suspend fun getAttendanceById(id: Long): AttendanceEntity?

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    fun observeAttendanceById(id: Long): Flow<AttendanceEntity?>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, createdAt DESC")
    fun getAttendanceHistory(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC, createdAt DESC")
    fun getAttendanceBySubject(subjectId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY createdAt DESC")
    fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceByDateSync(date: LocalDate): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY createdAt DESC")
    fun observeTodayAttendance(date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("""
        SELECT a.* FROM attendance_records a
        INNER JOIN subjects s ON a.subjectId = s.id
        WHERE s.name LIKE '%' || :query || '%' OR a.remarks LIKE '%' || :query || '%'
        ORDER BY a.date DESC, a.createdAt DESC
    """)
    fun searchAttendance(query: String): Flow<List<AttendanceEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM attendance_records 
            WHERE subjectId = :subjectId AND scheduleId = :scheduleId AND date = :date 
            LIMIT 1
        )
    """)
    suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean

    @Query("SELECT COUNT(*) FROM attendance_records WHERE status = 'PRESENT'")
    fun countPresent(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE status = 'ABSENT'")
    fun countAbsent(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE status = 'CANCELLED'")
    fun countCancelled(): Flow<Int>
}
