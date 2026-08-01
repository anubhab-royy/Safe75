package com.attendance.tracker.data.mapper

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.domain.model.AttendanceRecord
import com.attendance.tracker.feature.attendance.model.AttendanceUiModel
import java.time.LocalDate

/**
 * Mapper for converting Attendance record data structures between Database, Domain, and UI layers.
 */
object AttendanceMapper {

    /**
     * Converts a database [AttendanceEntity] to a domain [AttendanceRecord].
     */
    fun entityToDomain(entity: AttendanceEntity): AttendanceRecord {
        return AttendanceRecord(
            id = entity.id,
            subjectId = entity.subjectId,
            date = entity.date,
            status = entity.status,
            note = entity.note
        )
    }

    /**
     * Converts a domain [AttendanceRecord] to a database [AttendanceEntity].
     */
    fun domainToEntity(domain: AttendanceRecord): AttendanceEntity {
        return AttendanceEntity(
            id = domain.id,
            subjectId = domain.subjectId,
            date = domain.date,
            status = domain.status,
            note = domain.note
        )
    }

    /**
     * Converts a domain [AttendanceRecord] to an [AttendanceUiModel].
     */
    fun domainToUi(domain: AttendanceRecord): AttendanceUiModel {
        return AttendanceUiModel(
            id = domain.id,
            subjectId = domain.subjectId,
            date = domain.date.toString(),
            status = domain.status.name,
            note = domain.note
        )
    }

    /**
     * Converts an [AttendanceUiModel] to a domain [AttendanceRecord].
     */
    fun uiToDomain(uiModel: AttendanceUiModel): AttendanceRecord {
        return AttendanceRecord(
            id = uiModel.id,
            subjectId = uiModel.subjectId,
            date = LocalDate.parse(uiModel.date),
            status = AttendanceStatus.valueOf(uiModel.status),
            note = uiModel.note
        )
    }
}
