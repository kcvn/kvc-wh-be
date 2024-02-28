package com.kcvn.spm.common.helper

import org.jooq.TableField

class StringHelper {
    companion object {
        fun removeDecimalSuffix(value: String): String {
            return if (value.endsWith(".0")) {
                value.substringBeforeLast(".0")
            } else {
                value
            }
        }

        fun isIntField(field: TableField<*, *>): Boolean {
            if (field.type == Integer::class.java || field.type == Int::class.java) {
                return true
            }
            else {
                try {
                    field.cast(Int::class.java)
                    return true
                }
                catch (e: Exception) {
                    return false
                }
            }
        }

    }
}