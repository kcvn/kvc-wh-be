package com.kcvn.spm.common.helper

import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.payload.CalendarResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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

        fun convertStringToOffSetDateTime(date: String?,format :String = DateTimeFormat.dd_MM_yyyy): OffsetDateTime {
            val formatter = DateTimeFormatter.ofPattern(format)
            val localDate = LocalDate.parse(date, formatter)
            return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC)
        }

        fun convertStringToOffSetDateTime(date: String?, formats: List<String>): OffsetDateTime? {
            for (format in formats) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(format)
                    val localDate = LocalDate.parse(date, formatter)
                    return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC)
                } catch (e: DateTimeParseException) {
                    // If parsing fails, try the next format
                    continue
                }
            }
            return null
        }


        fun isFormatDate(date: String?, format: String): Boolean {
            return try {
                val formatter = DateTimeFormatter.ofPattern(format)
                LocalDate.parse(date, formatter)
                true
            } catch (e: Exception) {
                false
            }
        }
        fun isDateFormatDateCustom(date: String?): Boolean {
            return isFormatDate(date, DateTimeFormat.M_dd_yyyy) ||
                    isFormatDate(date, DateTimeFormat.yyyy_MM_dd) ||
                    isFormatDate(date, DateTimeFormat.dd_MM_yyyy)
        }


        fun toString(date: OffsetDateTime, format: String): String {
            return date.format(DateTimeFormatter.ofPattern(format))
        }

        fun toStringOrNull(date: OffsetDateTime?, format: String): String {
            if (date == null) return ""
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
//                        || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SATURDAY
//                        || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SUNDAY
                )
                calendarResponses.add(response)
                currentDate = currentDate.plusDays(1)
            }
            return calendarResponses
        }
    }
}