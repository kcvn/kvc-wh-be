package com.kcvn.spm.common.batch.excel

import org.springframework.lang.Nullable

/**
 * Interface to wrap different Excel implementations like JExcel or Apache POI.
 */

interface WorkSheet : Iterable<Array<String>?>, AutoCloseable {
    /**
     * Get the number of rows in this sheet.
     * @return the number of rows.
     */
    fun getNumberOfRows(): Int

    /**
     * Get the name of the sheet.
     * @return the name of the sheet.
     */
    fun getName(): String

    /**
     * Get the row as a `String[]`. Returns `null` if the row doesn't exist. Can throw an
     * `UnsupportedOperationException` when the underlying implementation doesn't support indexed access to rows.
     * @param rowNumber the row number to read.
     * @return a `String[]` or `null`
     */
    @Nullable
    fun getRow(rowNumber: Int): Array<String>?

    @Throws(Exception::class)
    override fun close() {
        //close function
    }
}
