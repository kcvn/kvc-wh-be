package com.kcvn.spm.common.batch.excel.rowset

import java.util.Properties

/**
 * Used by the [com.kcvn.spm.common.batch.excel.AbstractExcelItemReader]
 * to abstract away the complexities of the underlying Excel API implementations.
 */
interface RowSet {
    /**
     * Retrieves the metadata (name of the sheet, number of columns, names) of this row
     * set.
     * @return a corresponding `RowSetMetaData` instance.
     */
    val metaData: RowSetMetaData?

    /**
     * Move to the next row in the document.
     * @return `true` if the row is valid, `false` if there are no more rows
     */
    operator fun next(): Boolean

    /**
     * Returns the current row number.
     * @return the current row number
     */
    val currentRowIndex: Int

    /**
     * Return the current row as a `String[]`.
     * @return the row as a `String[]`
     */
    val currentRow: Array<String>?

    /**
     * Construct name-value pairs from the column names and string values. `null`
     * values are omitted.
     * @return some properties representing the row set.
     * @throws IllegalStateException if the column name meta data is not available.
     */
    fun getProperties(): Properties?
}
