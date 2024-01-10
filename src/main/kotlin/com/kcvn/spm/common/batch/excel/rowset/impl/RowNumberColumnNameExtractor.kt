package com.kcvn.spm.common.batch.excel.rowset.impl

import com.kcvn.spm.common.batch.excel.WorkSheet
import com.kcvn.spm.common.batch.excel.rowset.ColumnNameExtractor

/**
 * [ColumnNameExtractor] which returns the values of a given row (default is 0)
 * as the column names.
 */
class RowNumberColumnNameExtractor : ColumnNameExtractor {
    private var headerRowNumber = 0

    override fun getColumnNames(sheet: WorkSheet?): Array<String>? {
        return sheet!!.getRow(headerRowNumber)
    }

    fun setHeaderRowNumber(headerRowNumber: Int) {
        this.headerRowNumber = headerRowNumber
    }
}
