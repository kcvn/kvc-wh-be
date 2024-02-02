package com.kcvn.spm.common.helper.excelhelper

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row

class ExcelHelper {
    companion object {
        fun getCellValue(row: Row, colIdx: Int): String {
            try {
                val cell = row.getCell(colIdx)
                return when (cell.cellType) {
                    CellType.STRING -> cell.stringCellValue
                    CellType.NUMERIC -> cell.numericCellValue.toString()
                    CellType.BOOLEAN -> if (cell.booleanCellValue) "1" else "0"
                    else -> ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }
    }
}