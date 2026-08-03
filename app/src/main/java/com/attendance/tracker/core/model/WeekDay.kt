package com.attendance.tracker.core.model

/**
 * Represents standard days of the week.
 */
enum class WeekDay {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday;

    companion object {
        /** Days of the week ordered from Monday to Sunday for display. */
        val ordered: List<WeekDay> = values().toList()

        /** Maps a [java.time.DayOfWeek] to its [WeekDay] equivalent. */
        fun fromJavaDayOfWeek(dayOfWeek: java.time.DayOfWeek): WeekDay = when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> Monday
            java.time.DayOfWeek.TUESDAY -> Tuesday
            java.time.DayOfWeek.WEDNESDAY -> Wednesday
            java.time.DayOfWeek.THURSDAY -> Thursday
            java.time.DayOfWeek.FRIDAY -> Friday
            java.time.DayOfWeek.SATURDAY -> Saturday
            java.time.DayOfWeek.SUNDAY -> Sunday
        }
    }
}
