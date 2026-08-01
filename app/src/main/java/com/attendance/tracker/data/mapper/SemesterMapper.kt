package com.attendance.tracker.data.mapper

import com.attendance.tracker.data.local.database.entity.SemesterEntity
import com.attendance.tracker.domain.model.Semester
import com.attendance.tracker.feature.semester.model.SemesterUiModel
import java.time.LocalDate

/**
 * Mapper for converting Semester record data structures between Database, Domain, and UI layers.
 */
object SemesterMapper {

    /**
     * Converts a database [SemesterEntity] to a domain [Semester].
     */
    fun entityToDomain(entity: SemesterEntity): Semester {
        return Semester(
            id = entity.id,
            name = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate
        )
    }

    /**
     * Converts a domain [Semester] to a database [SemesterEntity].
     */
    fun domainToEntity(domain: Semester): SemesterEntity {
        return SemesterEntity(
            id = domain.id,
            name = domain.name,
            startDate = domain.startDate,
            endDate = domain.endDate
        )
    }

    /**
     * Converts a domain [Semester] to a [SemesterUiModel].
     */
    fun domainToUi(domain: Semester): SemesterUiModel {
        return SemesterUiModel(
            id = domain.id,
            name = domain.name,
            dateRangeDisplay = "${domain.startDate} - ${domain.endDate}"
        )
    }

    /**
     * Converts a [SemesterUiModel] to a domain [Semester].
     */
    fun uiToDomain(uiModel: SemesterUiModel): Semester {
        val parts = uiModel.dateRangeDisplay.split(" - ")
        val startDate = LocalDate.parse(parts.getOrElse(0) { LocalDate.now().toString() })
        val endDate = LocalDate.parse(parts.getOrElse(1) { LocalDate.now().toString() })
        return Semester(
            id = uiModel.id,
            name = uiModel.name,
            startDate = startDate,
            endDate = endDate
        )
    }
}
