package com.attendance.tracker.data.mapper

import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.feature.subject.model.SubjectUiModel

/**
 * Mapper for converting Subject data structures between Database, Domain, and UI layers.
 */
object SubjectMapper {

    /**
     * Converts a database [SubjectEntity] to a domain [Subject].
     */
    fun entityToDomain(entity: SubjectEntity): Subject {
        return Subject(
            id = entity.id,
            name = entity.name,
            facultyName = entity.facultyName,
            color = entity.color,
            requiredAttendancePercentage = entity.requiredAttendancePercentage,
            personalAttendanceGoal = entity.personalAttendanceGoal,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a domain [Subject] to a database [SubjectEntity].
     */
    fun domainToEntity(domain: Subject): SubjectEntity {
        return SubjectEntity(
            id = domain.id,
            name = domain.name,
            facultyName = domain.facultyName,
            color = domain.color,
            requiredAttendancePercentage = domain.requiredAttendancePercentage,
            personalAttendanceGoal = domain.personalAttendanceGoal,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Converts a domain [Subject] to a [SubjectUiModel].
     */
    fun domainToUi(domain: Subject): SubjectUiModel {
        return SubjectUiModel(
            id = domain.id,
            name = domain.name,
            faculty = domain.facultyName,
            requiredAttendance = domain.requiredAttendancePercentage,
            attendanceGoal = domain.personalAttendanceGoal,
            color = domain.color
        )
    }

    /**
     * Converts a [SubjectUiModel] to a domain [Subject].
     */
    fun uiToDomain(uiModel: SubjectUiModel): Subject {
        return Subject(
            id = uiModel.id,
            name = uiModel.name,
            facultyName = uiModel.faculty,
            color = uiModel.color,
            requiredAttendancePercentage = uiModel.requiredAttendance,
            personalAttendanceGoal = uiModel.attendanceGoal
        )
    }
}
