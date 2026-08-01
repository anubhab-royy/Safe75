package com.attendance.tracker.data.mapper

import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import com.attendance.tracker.domain.model.SemesterVersion

/**
 * Mapper for converting Semester Version data structures between Database and Domain layers.
 */
object SemesterMapper {
    fun entityToDomain(entity: SemesterVersionEntity): SemesterVersion {
        return SemesterVersion(
            id = entity.id,
            name = entity.name,
            isActive = entity.isActive,
            createdAt = entity.createdAt
        )
    }

    fun domainToEntity(domain: SemesterVersion): SemesterVersionEntity {
        return SemesterVersionEntity(
            id = domain.id,
            name = domain.name,
            isActive = domain.isActive,
            createdAt = domain.createdAt
        )
    }
}
