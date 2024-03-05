package com.kcvn.spm.common.helper

import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.util.CommonUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ExcelHelper {
    companion object {
        fun getCellValue(row: Row, colIdx: Int, format: String? = null): String {
            try {
                val cell = row.getCell(colIdx)
                if (!format.isNullOrEmpty() && cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    val dateFormat = SimpleDateFormat(format)
                    val date = cell.dateCellValue
                    return dateFormat.format(date)
                }
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

        fun setCellValue(row: Row, colIndex: Int, style: CellStyle, value: String?) {
            row.createCell(colIndex).setCellValue(value)
            row.getCell(colIndex).cellStyle = style
        }

        fun columnIsMatchingTemplate(templateUrl: String, headerRowImport: Row, indexHeaderRow: Int, rangeCheckCol: Int?): Boolean {
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

        fun fileIsEmpty(sheet: Sheet, rowIndex: Int): Boolean {
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

        fun checkCalendarColumn(headerRowImport: Row, startCol: Int, endCol: Int, formats: Array<String>): Boolean {
            for (i in startCol until endCol + 1) {
                val cell = headerRowImport.getCell(i)
                if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    continue
                }
                val cellValue = getCellValue(headerRowImport, i)
                if (cellValue.isEmpty()) return false
                var isDate = false
                for (format in formats) {
                    val formatter = DateTimeFormatter.ofPattern(format)
                    try {
                        formatter.parse(cellValue)
                        isDate = true
                        break
                    } catch (e: DateTimeParseException) {
                        continue
                    }
                }
                if (!isDate) return false
            }
            return true
        }

        fun getCellStyleResultCol(workbook: Workbook, styleTemplate: CellStyle, hasFontColor: Boolean = true): CellStyle {
            val cellStyle = workbook.createCellStyle()
            if (!hasFontColor) {
                val font = workbook.createFont()
                font.color = IndexedColors.RED.index
                cellStyle.setFont(font)
            }
            cellStyle.alignment = HorizontalAlignment.LEFT
            cellStyle.borderTop = styleTemplate.borderTop
            cellStyle.borderLeft = BorderStyle.THIN
            cellStyle.borderRight = BorderStyle.THIN
            cellStyle.borderBottom = styleTemplate.borderBottom
            return cellStyle
        }

        fun getCellStyleCommon(workbook: Workbook): CellStyle {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = ExcelConstant.FONT_TIMES_NEW_ROMAN
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)
            return style
        }

        fun createColResult(headerRow: Row, sheet: Sheet): Int {
            val colEmpty = headerRow.firstOrNull { x -> getCellValue(headerRow, x.columnIndex) == "" }
            val colResult = headerRow.firstOrNull { x -> getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
            val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (headerRow.lastCellNum + 0))

            if (colResult == null) {
                headerRow.createCell(colIndexResult).setCellValue(CommonUtils.getMessage("excel.colResultName"))
            } else {
                headerRow.getCell(colIndexResult).setCellValue(CommonUtils.getMessage("excel.colResultName"))
            }
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(colIndexResult).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(colIndexResult, 15000)

            return colIndexResult
        }
    }
}