package com.attendance.tracker.data.mapper

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.feature.schedule.model.ScheduleUiModel
import java.time.LocalTime

/**
 * Mapper for converting Schedule data structures between Database, Domain, and UI layers.
 */
object ScheduleMapper {

    /**
     * Converts a database [ScheduleEntity] to a domain [Schedule].
     */
    fun entityToDomain(entity: ScheduleEntity): Schedule {
        return Schedule(
            id = entity.id,
            subjectId = entity.subjectId,
            dayOfWeek = entity.dayOfWeek,
            startTime = entity.startTime,
            endTime = entity.endTime,
            room = entity.room
        )
    }

    /**
     * Converts a domain [Schedule] to a database [ScheduleEntity].
     */
    fun domainToEntity(domain: Schedule): ScheduleEntity {
        return ScheduleEntity(
            id = domain.id,
            subjectId = domain.subjectId,
            dayOfWeek = domain.dayOfWeek,
            startTime = domain.startTime,
            endTime = domain.endTime,
            room = domain.room
        )
    }

    /**
     * Converts a domain [Schedule] to a [ScheduleUiModel].
     */
    fun domainToUi(domain: Schedule): ScheduleUiModel {
        return ScheduleUiModel(
            id = domain.id,
            subjectId = domain.subjectId,
            dayOfWeek = domain.dayOfWeek.name,
            startTime = domain.startTime.toString(),
            endTime = domain.endTime.toString(),
            room = domain.room
        )
    }

    /**
     * Converts a [ScheduleUiModel] to a domain [Schedule].
     */
    fun uiToDomain(uiModel: ScheduleUiModel): Schedule {
        return Schedule(
            id = uiModel.id,
            subjectId = uiModel.subjectId,
            dayOfWeek = WeekDay.valueOf(uiModel.dayOfWeek),
            startTime = LocalTime.parse(uiModel.startTime),
            endTime = LocalTime.parse(uiModel.endTime),
            room = uiModel.room
        )
    }
}
