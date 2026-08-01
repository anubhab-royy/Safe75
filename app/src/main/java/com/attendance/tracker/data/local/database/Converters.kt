package com.attendance.tracker.data.local.database

import androidx.room.TypeConverter
import com.attendance.tracker.core.model.WeekDay
import java.time.LocalDate
import java.time.LocalTime

/**
 * Type Converters for Room to handle serialization of Java 8 date/time types and enums.
 */
class Converters {
    @TypeConverter
    fun fromWeekDay(value: WeekDay?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWeekDay(value: String?): WeekDay? {
        return value?.let { WeekDay.valueOf(it) }
    }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
}
