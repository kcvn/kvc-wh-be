package com.kcvn.spm.common.batch.excel.poi

import com.kcvn.spm.common.batch.excel.WorkSheet
import org.apache.poi.ss.usermodel.*
import org.springframework.lang.Nullable

/**
 * Sheet implementation for Apache POI.
 */
class PoiWorkSheet(private val delegate: Sheet, private val dataFormatter: DataFormatter) : WorkSheet {
    private val numberOfRows: Int = delegate.lastRowNum + 1
    private val name: String = delegate.sheetName
    private var evaluator: FormulaEvaluator? = null

    /**
     * {@inheritDoc}
     */
    override fun getNumberOfRows(): Int {
        return numberOfRows
    }

    /**
     * {@inheritDoc}
     */
    override fun getName(): String {
        return name
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    override fun getRow(rowNumber: Int): Array<String>? {
        val row = delegate.getRow(rowNumber)
        return map(row)
    }

    @Nullable
    private fun map(row: Row?): Array<String>? {
        if (row == null) {
            return null
        }
        val cells: MutableList<String> = ArrayList()
        val numberOfColumns = row.lastCellNum.toInt()
        for (i in 0 until numberOfColumns) {
            val cell = row.getCell(i)
            val cellType = cell.cellType
            if (cellType == CellType.FORMULA) {
                cells.add(dataFormatter.formatCellValue(cell, getFormulaEvaluator()))
            } else {
                cells.add(dataFormatter.formatCellValue(cell))
            }
        }
        return cells.toTypedArray<String>()
    }

    /**
     * Lazy getter for the `FormulaEvaluator`. Takes some time to create an
     * instance, so if not necessary don't create it.
     * @return the `FormulaEvaluator`
     */
    private fun getFormulaEvaluator(): FormulaEvaluator? {
        if (evaluator == null) {
            evaluator = delegate.workbook.creationHelper.createFormulaEvaluator()
        }
        return evaluator
    }

    override fun iterator(): Iterator<Array<String>?> {
        return object : Iterator<Array<String>?> {
            private val delegateIterator: Iterator<Row> = delegate.iterator()
            override fun hasNext(): Boolean {
                return delegateIterator.hasNext()
            }

            override fun next(): Array<String>? {
                return map(delegateIterator.next())!!
            }
        }
    }
}
