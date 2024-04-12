package com.kcvn.spm.common.helper

import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.payload.CalendarResponse
import java.time.*
import java.time.format.DateTimeFormatter

class DateTimeHelper {
    companion object {
        fun getFirstDayOfQuarterInYear(date: LocalDateTime): LocalDateTime? {
            val year = date.year
            return when (date.month.value) {
                1 -> LocalDateTime.of(year, 1, 1, 0, 0)
                2 -> LocalDateTime.of(year, 1, 1, 0, 0)
                3 -> LocalDateTime.of(year, 1, 1, 0, 0)
                4 -> LocalDateTime.of(year, 4, 1, 0, 0)
                5 -> LocalDateTime.of(year, 4, 1, 0, 0)
                6 -> LocalDateTime.of(year, 4, 1, 0, 0)
                7 -> LocalDateTime.of(year, 7, 1, 0, 0)
                8 -> LocalDateTime.of(year, 7, 1, 0, 0)
                9 -> LocalDateTime.of(year, 7, 1, 0, 0)
                10 -> LocalDateTime.of(year, 10, 1, 0, 0)
                11 -> LocalDateTime.of(year, 10, 1, 0, 0)
                12 -> LocalDateTime.of(year, 10, 1, 0, 0)
                else -> null
            }
        }

        fun convertOffSetDateTimeUtc7ToString(date: OffsetDateTime?): String? {
            val dateUtc7 = date?.plusHours(7)
            val localDateTime = dateUtc7?.atZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
             return  localDateTime?.format(formatter)
        }

        fun convertOffSetDateTimeToLocalDateTimeToString(date: OffsetDateTime?): String? {
            val localDateTime = date?.toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
            return  localDateTime?.format(formatter)
        }

        fun convertStringToOffSetDateTime(date: String): OffsetDateTime {
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
            val localDate = LocalDate.parse(date, formatter)
            return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC)
        }

        fun toString(date: OffsetDateTime, format: String): String {
            return date.format(DateTimeFormatter.ofPattern(format))

        }

        fun toString(date: LocalDateTime, format: String): String {
            return date.format(DateTimeFormatter.ofPattern(format))
        }

        fun toTimeZone7(date: OffsetDateTime?): OffsetDateTime? {
            return date?.plusHours(7)
        }

        fun toTimeZone7toString(date: OffsetDateTime, format: String): String {
            return date.plusHours(7).format(DateTimeFormatter.ofPattern(format))
        }

        fun toUniversalTime(date: LocalDateTime): OffsetDateTime {
            return OffsetDateTime.of(date, ZoneOffset.UTC).plusHours(-7)
        }

        fun toUniversalTime(date: OffsetDateTime): OffsetDateTime {
            return date.plusHours(-7)
        }

        fun toCalendarColumn(startDate: OffsetDateTime, endDate: OffsetDateTime, holidayCalender: List<OffsetDateTime> = listOf(),daysToSubtract: Long = 0): List<CalendarResponse> {
            val calendarResponses = mutableListOf<CalendarResponse>()
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val response = CalendarResponse(
                    key = toString(currentDate, DateTimeFormat.yyyyMMdd),
                    value = toString(currentDate.minusDays(daysToSubtract), DateTimeFormat.MM_dd),
                    isHoliday = holidayCalender.any { toTimeZone7(it)?.toLocalDate() == currentDate.toLocalDate() }
                        || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SATURDAY
                        || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SUNDAY
                )
                calendarResponses.add(response)
                currentDate = currentDate.plusDays(1)
            }
            return calendarResponses
        }
    }
}