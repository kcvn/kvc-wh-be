package com.kcvn.spm.common.batch.excel.rowset.impl

import com.kcvn.spm.common.batch.excel.WorkSheet
import com.kcvn.spm.common.batch.excel.rowset.ColumnNameExtractor
import com.kcvn.spm.common.batch.excel.rowset.RowSet
import com.kcvn.spm.common.batch.excel.rowset.RowSetFactory

/**
 * [RowSetFactory] implementation which constructs a [DefaultRowSet] instance
 * and [DefaultRowSetMetaData] instance. The latter will have the
 * [ColumnNameExtractor] configured on this factory set (default
 * [RowNumberColumnNameExtractor]).
 */
class DefaultRowSetFactory : RowSetFactory {
    private var columnNameExtractor: ColumnNameExtractor = RowNumberColumnNameExtractor()

    override fun create(sheet: WorkSheet?): RowSet {
        val metaData = DefaultRowSetMetaData(sheet!!, columnNameExtractor)
        return DefaultRowSet(sheet, metaData)
    }

    fun setColumnNameExtractor(columnNameExtractor: ColumnNameExtractor) {
        this.columnNameExtractor = columnNameExtractor
    }


}
