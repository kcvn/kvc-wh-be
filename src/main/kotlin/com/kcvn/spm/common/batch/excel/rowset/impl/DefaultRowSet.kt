package com.kcvn.spm.common.batch.excel.rowset.impl

import com.kcvn.spm.common.batch.excel.WorkSheet
import com.kcvn.spm.common.batch.excel.rowset.RowSet
import com.kcvn.spm.common.batch.excel.rowset.RowSetMetaData
import java.util.*

/**
 * Default implementation of the [RowSet] interface.
 *
 * @see DefaultRowSetFactory
 */
class DefaultRowSet internal constructor(
    sheet: WorkSheet,
    override val metaData: RowSetMetaData
) : RowSet {
    private val sheetData: Iterator<Array<String>?>
    override var currentRowIndex = -1
    override var currentRow: Array<String>? = null

    init {
        sheetData = sheet.iterator()
    }

    override fun next(): Boolean {
        currentRow = null
        currentRowIndex++
        if (sheetData.hasNext()) {
            currentRow = sheetData.next()
            return true
        }
        return false
    }

    override fun getProperties(): Properties {
        val names = metaData.getColumnNames()
            ?: throw IllegalStateException("Cannot create properties without meta data")
        val props = Properties()
        for (i in currentRow!!.indices) {
            val value = currentRow!![i]
            if (value != null) {
                props.setProperty(names[i], value)
            }
        }
        return props
    }
}
