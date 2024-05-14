package com.kcvn.spm.common.helper

import java.math.BigDecimal
import java.math.RoundingMode

class NumberHelper {
    companion object {
        fun truncateDecimal(input: BigDecimal): BigDecimal {
            return input.setScale(2, RoundingMode.HALF_UP)
        }

        fun formatDoubleValue(value: Double): String {
            return String.format("%.1f", value)
        }

        fun roundedUp(number: BigDecimal): Int {
            val intNumber = number.toInt()
            return if (BigDecimal(intNumber) == number) intNumber else intNumber + 1
        }

        fun isNumeric(input: String): Boolean {
            return input.toDoubleOrNull() != null
        }
    }

}