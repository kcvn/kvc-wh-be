package com.kcvn.spm.common.helper

class StringHelper {
    fun removeDecimalSuffix(value: String): String {
        return if (value.endsWith(".0")) {
            value.substringBeforeLast(".0")
        } else {
            value
        }
    }

}