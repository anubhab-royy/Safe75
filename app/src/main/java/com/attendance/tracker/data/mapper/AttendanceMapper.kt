package com.attendance.tracker.data.mapper

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.feature.attendance.model.AttendanceUiModel
import java.time.LocalDate

/**
 * Mapper for converting Attendance record data structures between Database, Domain, and UI layers.
 */
object AttendanceMapper {

    /**
     * Converts a database [AttendanceEntity] to a domain [Attendance].
     */
    fun entityToDomain(entity: AttendanceEntity): Attendance {
        return Attendance(
            id = entity.id,
            subjectId = entity.subjectId,
            scheduleId = entity.scheduleId,
            date = entity.date,
            status = entity.status,
            remarks = entity.remarks,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a domain [Attendance] to a database [AttendanceEntity].
     */
    fun domainToEntity(domain: Attendance): AttendanceEntity {
        return AttendanceEntity(
            id = domain.id,
            subjectId = domain.subjectId,
            scheduleId = domain.scheduleId,
            date = domain.date,
            status = domain.status,
            remarks = domain.remarks,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Converts a domain [Attendance] to an [AttendanceUiModel] with optional subject/schedule details.
     */
    fun domainToUi(
        domain: Attendance,
        subjectName: String = "",
        subjectColor: Int = 0xFF9E9E9E.toInt(),
        timeRange: String = ""
    ): AttendanceUiModel {
        return AttendanceUiModel(
            id = domain.id,
            subjectId = domain.subjectId,
            scheduleId = domain.scheduleId,
            date = domain.date.toString(),
            status = domain.status.name,
            remarks = domain.remarks,
            subjectName = subjectName,
            subjectColor = subjectColor,
            timeRange = timeRange
        )
    }

    /**
     * Converts an [AttendanceUiModel] to a domain [Attendance].
     */
    fun uiToDomain(uiModel: AttendanceUiModel): Attendance {
        return Attendance(
            id = uiModel.id,
            subjectId = uiModel.subjectId,
            scheduleId = uiModel.scheduleId,
            date = LocalDate.parse(uiModel.date),
            status = AttendanceStatus.valueOf(uiModel.status),
            remarks = uiModel.remarks
        )
    }
}
