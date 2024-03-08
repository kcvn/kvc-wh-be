package com.kcvn.spm.common.helper

import java.math.BigDecimal
import java.math.RoundingMode

class NumberHelper {
    companion object {
        fun truncateDecimal(input: BigDecimal): BigDecimal {
            return input.setScale(2, RoundingMode.HALF_UP)
        }
    }

}