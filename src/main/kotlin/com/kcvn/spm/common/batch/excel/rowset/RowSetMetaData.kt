package com.kcvn.spm.common.batch.excel.rowset

/**
 * Interface representing the metadata associated with an Excel document.
 */
interface RowSetMetaData {
    /**
     * Retrieves the names of the columns for the current sheet.
     * @return the column names.
     */
    fun getColumnNames(): Array<String>?

    /**
     * Retrieves the name of the sheet the RowSet is based on.
     * @return the name of the sheet
     */
    fun getSheetName(): String?
}
