package com.kcvn.spm.common.batch.excel.rowset

import com.kcvn.spm.common.batch.excel.WorkSheet

/**
 * Contract for factories which will construct a [RowSet] implementation.
 */
interface RowSetFactory {
    /**
     * Create a RowSet instance.
     * @param sheet an Excel sheet.
     * @return a `RowSet` instance.
     */
    fun create(sheet: WorkSheet?): RowSet?
}
