package com.kcvn.spm.common.batch.excel

import com.kcvn.spm.common.batch.excel.rowset.RowSet

/**
 * Callback to handle skipped lines. Useful for header/footer processing.
 */
interface RowCallbackHandler {
    /**
     * Implementations must implement this method to process each row of data in the [RowSet].
     *
     * This method should not call `next()` on the [RowSet]; it is only
     * supposed to extract values of the current row.
     *
     * Exactly what the implementation chooses to do is up to it: A trivial implementation
     * might simply count rows, while another implementation might build a special header
     * row.
     * @param rs the `RowSet` to process (preset at the current row)
     */
    fun handleRow(rs: RowSet?)
}
