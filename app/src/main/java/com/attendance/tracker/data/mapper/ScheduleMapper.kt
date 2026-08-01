package com.attendance.tracker.data.mapper

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.feature.schedule.model.ScheduleUiModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Mapper for converting Schedule data structures between Database, Domain, and UI layers.
 */
object ScheduleMapper {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun entityToDomain(entity: ScheduleEntity): Schedule {
        return Schedule(
            id = entity.id,
            subjectId = entity.subjectId,
            dayOfWeek = entity.dayOfWeek,
            startTime = entity.startTime,
            endTime = entity.endTime,
            room = entity.room,
            teacherOverride = entity.teacherOverride,
            versionId = entity.versionId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun domainToEntity(domain: Schedule): ScheduleEntity {
        return ScheduleEntity(
            id = domain.id,
            subjectId = domain.subjectId,
            dayOfWeek = domain.dayOfWeek,
            startTime = domain.startTime,
            endTime = domain.endTime,
            room = domain.room,
            teacherOverride = domain.teacherOverride,
            versionId = domain.versionId,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun domainToUi(
        domain: Schedule,
        subjectName: String,
        subjectColor: Int,
        subjectFaculty: String?
    ): ScheduleUiModel {
        return ScheduleUiModel(
            id = domain.id,
            subjectId = domain.subjectId,
            subjectName = subjectName,
            subjectColor = subjectColor,
            dayOfWeek = domain.dayOfWeek.name,
            startTime = domain.startTime.format(timeFormatter),
            endTime = domain.endTime.format(timeFormatter),
            room = domain.room,
            faculty = domain.teacherOverride ?: subjectFaculty
        )
    }

    fun uiToDomain(uiModel: ScheduleUiModel, versionId: Long): Schedule {
        return Schedule(
            id = uiModel.id,
            subjectId = uiModel.subjectId,
            dayOfWeek = WeekDay.valueOf(uiModel.dayOfWeek),
            startTime = LocalTime.parse(uiModel.startTime),
            endTime = LocalTime.parse(uiModel.endTime),
            room = uiModel.room,
            teacherOverride = uiModel.faculty,
            versionId = versionId
        )
    }
}
