package com.kcvn.spm.common.helper

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

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

        fun divide(number1: BigDecimal, number2: BigDecimal): BigDecimal {
            return number1.divide(number2, 2, RoundingMode.HALF_UP)
        }

        fun divide(number1: Int, number2: Int): BigDecimal {
            return BigDecimal(number1).divide(BigDecimal(number2), 2, RoundingMode.HALF_UP)
        }

        fun formatNumber(number: Int?): String {
            if (number == null) return ""
            val numberFormat = NumberFormat.getNumberInstance(Locale.US)
            return numberFormat.format(number)
        }
    }

}