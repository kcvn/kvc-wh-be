package com.kcvn.spm.common.helper

class StringHelper {
    companion object {
        fun removeDecimalSuffix(value: String): String {
            return if (value.endsWith(".0")) {
                value.substringBeforeLast(".0")
            } else {
                value
            }
        }

        fun intToStringD2(number: String?): String {
            return number?.toBigDecimalOrNull()?.toInt().toString().padStart(2, '0') ?: ""
        }

        fun intToStringD2(number: Int?): String {
            return number?.toString()?.padStart(2, '0') ?: ""
        }

    }
}