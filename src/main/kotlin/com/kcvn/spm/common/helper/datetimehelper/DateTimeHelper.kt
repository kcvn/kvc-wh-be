package com.kcvn.spm.common.helper.datetimehelper

import java.time.LocalDateTime

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
    }
}