package com.kcvn.spm.common.helper.excelhelper

import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream

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

        fun columnIsMatchingTemplate(templateUrl: String, headerRowImport: Row, indexHeaderRow: Int, rangeCheckCol: Int?) : Boolean {
            val workbookTemplate = FileInputStream(templateUrl).use { x -> XSSFWorkbook(x) }
            val headerRowTemplate = workbookTemplate.getSheetAt(0).getRow(indexHeaderRow)

            val rangeCol = rangeCheckCol ?: (headerRowImport.physicalNumberOfCells + 0)

            for (i in 0 until rangeCol) {
                val templateCellValue = getCellValue(headerRowTemplate, i).lowercase()
                val importCellValue = getCellValue(headerRowImport, i).lowercase()

                if (templateCellValue != importCellValue) {
                    return false
                }
            }
            workbookTemplate.close()
            return true
        }

        fun fileIsEmpty(sheet: Sheet, rowIndex: Int) : Boolean {
            val countRowCheck = if (sheet.lastRowNum < 5) sheet.lastRowNum else 5
            var isEmpty = true
            for (iRow in rowIndex until countRowCheck + 1) {
                val row = sheet.getRow(iRow)
                for (iCol in 0 until row.physicalNumberOfCells) {
                    if (getCellValue(row, iCol).isNotEmpty()) {
                        isEmpty = false
                        break
                    }
                }
                if (!isEmpty) break
            }

            return isEmpty
        }
    }
}