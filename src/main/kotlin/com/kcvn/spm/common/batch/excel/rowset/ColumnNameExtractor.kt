package com.kcvn.spm.common.batch.excel.rowset

import com.kcvn.spm.common.batch.excel.WorkSheet

/**
 * Contract for extracting column names for a given [WorkSheet].
 */
interface ColumnNameExtractor {
    /**
     * Retrieves the names of the columns in the given `Sheet`.
     * @param sheet the sheet
     * @return the column names
     */
    fun getColumnNames(sheet: WorkSheet?): Array<String>?
}
