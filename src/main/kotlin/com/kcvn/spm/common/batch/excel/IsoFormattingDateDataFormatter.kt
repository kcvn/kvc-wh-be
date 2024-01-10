package com.kcvn.spm.common.batch.excel

import org.apache.poi.ss.formula.ConditionalFormattingEvaluator
import org.apache.poi.ss.usermodel.*
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Specialized subclass for additionally formatting the date into an ISO date/time.
 *
 * @see DateTimeFormatter.ISO_OFFSET_DATE_TIME
 */
class IsoFormattingDateDataFormatter : DataFormatter {
    constructor() : super()
    constructor(locale: Locale?) : super(locale)

    override fun formatCellValue(
        cell: Cell?,
        evaluator: FormulaEvaluator?,
        cfEvaluator: ConditionalFormattingEvaluator
    ): String {
        if (cell == null) {
            return ""
        }
        var cellType = cell.cellType
        if (cellType == CellType.FORMULA) {
            if (evaluator == null) {
                return cell.cellFormula
            }
            cellType = evaluator.evaluateFormulaCell(cell)
        }
        if (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell, cfEvaluator)) {
            val value = cell.localDateTimeCellValue
            return if (value != null) value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) else ""
        }
        return super.formatCellValue(cell, evaluator, cfEvaluator)
    }
}
