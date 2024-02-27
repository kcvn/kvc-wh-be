package com.kcvn.spm.common.batch.excel

import com.kcvn.spm.common.batch.excel.rowset.RowSet

/**
 * Map rows from an Excel sheet to an object.
 *
 * @param <T> the type
 */
interface RowMapper<T> {
    /**
     * Implementations must implement this method to map the provided row to the parameter
     * type T. The row number represents the number of rows into a [WorkSheet] the
     * current line resides.
     * @param rs the RowSet used for mapping.
     * @return mapped object of type T
     * @throws Exception if error occurred while parsing.
     */
    @Throws(Exception::class)
    fun mapRow(rs: RowSet): T
}
