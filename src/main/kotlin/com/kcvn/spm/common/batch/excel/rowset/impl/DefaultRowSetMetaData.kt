package com.kcvn.spm.common.batch.excel.rowset.impl

import com.kcvn.spm.common.batch.excel.WorkSheet
import com.kcvn.spm.common.batch.excel.rowset.ColumnNameExtractor
import com.kcvn.spm.common.batch.excel.rowset.RowSetMetaData

/**
 * Default implementation for the [RowSetMetaData] interface.
 * Requires a [WorkSheet] and [ColumnNameExtractor] to operate correctly.
 * Delegates the retrieval of the column names to the [ColumnNameExtractor].
 */
class DefaultRowSetMetaData internal constructor(
    private val sheet: WorkSheet,
    private val columnNameExtractor: ColumnNameExtractor
) : RowSetMetaData {
    private var columnNames: Array<String>? = null

    override fun getColumnNames(): Array<String>? {
        if (columnNames == null) {
            columnNames = columnNameExtractor.getColumnNames(sheet)
        }
        return columnNames!!
    }

    override fun getSheetName(): String {
        return sheet.getName()
    }
}
