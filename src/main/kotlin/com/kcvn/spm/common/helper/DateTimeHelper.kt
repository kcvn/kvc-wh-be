package com.kcvn.spm.common.helper

import com.kcvn.spm.common.constants.DateTimeFormat
import java.time.*
import java.time.format.DateTimeFormatter

class DateTimeHelper {
    companion object {
        fun getFirstDayOfQuarterInYear(date: LocalDateTime) : LocalDateTime? {
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

        fun convertOffSetDateTimeUtc7ToString(date: OffsetDateTime?) : String? {
            val dateUtc7 = date?.plusHours(7)
            val localDateTime = dateUtc7?.atZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
            val formattedDateTime = localDateTime?.format(formatter)
            return formattedDateTime
        }

        fun convertOffSetDateTimeToLocalDateTimeToString(date: OffsetDateTime?) : String? {
            val localDateTime = date?.toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
            val formattedDateTime = localDateTime?.format(formatter)
            return formattedDateTime
        }

        fun convertStringToOffSetDateTime(date: String) : OffsetDateTime{
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
            val localDate = LocalDate.parse(date, formatter)
            val offsetDateTime = OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC)
            return offsetDateTime
        }

        fun toString(offsetDateTime: OffsetDateTime, format: String): String {
            return  offsetDateTime.format(DateTimeFormatter.ofPattern(format))

        }
        fun convertDateUtc7(date: OffsetDateTime?): OffsetDateTime? {
            return date?.plusHours(7)
        }

        fun convertDateDbUtc7(date: OffsetDateTime?): OffsetDateTime? {
            return date?.minusHours(7)
        }

    }
}