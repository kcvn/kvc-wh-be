package com.kcvn.spm.common.batch.excel.rowset.impl

import com.kcvn.spm.common.batch.excel.WorkSheet
import com.kcvn.spm.common.batch.excel.rowset.ColumnNameExtractor
import java.util.Arrays

/**
 * [ColumnNameExtractor] implementation which returns a preset String[] to use as the column names.
 * Useful for those situations in which an Excel file without a header row is read.
 */
class StaticColumnNameExtractor(private val columnNames: Array<String>) : ColumnNameExtractor {
    override fun getColumnNames(sheet: WorkSheet?): Array<String> {
        return Arrays.copyOf(columnNames, columnNames.size)
    }
}
