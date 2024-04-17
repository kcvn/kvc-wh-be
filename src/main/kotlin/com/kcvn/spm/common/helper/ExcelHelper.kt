package com.kcvn.spm.common.helper

import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.util.CommonUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.constants.Color
import java.text.DecimalFormat

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
        fun getCellValueCustom(row: Row, columnIndex: Int): String {
            val cell = row.getCell(columnIndex)
            return when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> if(DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    DecimalFormat("0.####").format(cell.numericCellValue)
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> cell.cellFormula
                else -> ""
            }
        }
        fun setCellValue(row: Row, colIndex: Int, styleTemplate: CellStyle, value: String?) {
            row.createCell(colIndex).setCellValue(value)
            row.getCell(colIndex).cellStyle = styleTemplate
        }

        fun setCellValue(row: Row, colIndex: Int, styleTemplate: CellStyle, value: Date?) {
            row.createCell(colIndex).setCellValue(value)
            row.getCell(colIndex).cellStyle = styleTemplate
        }

        fun setCellValueWithCalendar(workbook: Workbook, row: Row, colIndex: Int, style: CellStyle, value: String?, isHoliday: Boolean = false, color: String? = null, isReportDetails: Boolean = false) {
            row.createCell(colIndex).setCellValue(value)
            val cellStyle = workbook.createCellStyle()
            cellStyle.cloneStyleFrom(style)
            cellStyle.alignment = HorizontalAlignment.CENTER
            cellStyle.borderTop = style.borderTop
            cellStyle.borderLeft = BorderStyle.THIN
            cellStyle.borderRight = BorderStyle.THIN
            cellStyle.borderBottom = style.borderBottom

            if(isReportDetails){
                cellStyle.borderTop = BorderStyle.THIN
                cellStyle.borderBottom = BorderStyle.THIN

            }
            if (isHoliday) {
                cellStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            if(!color.isNullOrEmpty()){
                if(color == Color.YELLOW){
                    cellStyle.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
                }
                else if(color == Color.ORANGE){
                    cellStyle.fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
                }
            }
            row.getCell(colIndex).cellStyle = cellStyle
        }

        fun setCellValueCustom(
            workbook: Workbook,
            row: Row,
            colIndex: Int,
            styleTemplate: CellStyle,
            value: String?,
            isBorderLeft: Boolean = true,
            isBorderRight: Boolean = true,
            isBorderTop: Boolean = true,
            isBorderBottom: Boolean = true,
            isBold: Boolean = false,
            isAlignCenter: Boolean = false,
            indexColor: Short? = null,
            isNumberFormat: Boolean = false
            ) {
            val style = workbook.createCellStyle()
            style.cloneStyleFrom(styleTemplate)
            row.createCell(colIndex).setCellValue(value)

            if (isBorderLeft) style.borderLeft = BorderStyle.THIN else style.borderLeft = BorderStyle.NONE
            if (isBorderRight) style.borderRight = BorderStyle.THIN else style.borderRight = BorderStyle.NONE
            if (isBorderTop) style.borderTop = BorderStyle.THIN else style.borderTop = BorderStyle.NONE
            if (isBorderBottom) style.borderBottom = BorderStyle.THIN else style.borderBottom = BorderStyle.NONE
            if (isAlignCenter) style.alignment = HorizontalAlignment.CENTER
            if (indexColor != null) {
                style.fillForegroundColor = indexColor
                style.fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            if (isBold) {
                val fontTemplate = workbook.getFontAt(styleTemplate.fontIndex)
                val font = workbook.createFont()
                font.fontName = fontTemplate.fontName
                font.fontHeightInPoints = fontTemplate.fontHeightInPoints
                font.bold = true
                style.setFont(font)
            }
            if(isNumberFormat){
                val dataFormat = workbook.createDataFormat()
                style.dataFormat = dataFormat.getFormat("#,##0")
                val numberValue = value?.toDouble() ?: 0.0
                row.createCell(colIndex).setCellValue(numberValue)
            }
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

        fun setCellHeaderStyle(workbook: Workbook): CellStyle {
            val style: CellStyle = workbook.createCellStyle()
            style.alignment = HorizontalAlignment.CENTER
            style.verticalAlignment = VerticalAlignment.CENTER
            style.borderTop = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderBottom = BorderStyle.THIN
            style.fillPattern = FillPatternType.NO_FILL
            val font: Font = workbook.createFont()
            font.fontName = ExcelConstant.FONT_TIMES_NEW_ROMAN
            font.bold = true
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)
            return style
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
            cellStyle.cloneStyleFrom(styleTemplate)
            val fontTemplate = workbook.getFontAt(styleTemplate.fontIndex)
            val font = workbook.createFont()
            font.fontName = fontTemplate.fontName
            font.fontHeightInPoints = fontTemplate.fontHeightInPoints
            if (hasFontColor) {
                font.color = IndexedColors.RED.index
                cellStyle.setFont(font)
            }
            cellStyle.verticalAlignment = VerticalAlignment.CENTER
            cellStyle.alignment = HorizontalAlignment.LEFT
            cellStyle.wrapText = true
            return cellStyle
        }

        fun getCellStyleCommon(workbook: Workbook): CellStyle {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true
            style.verticalAlignment = VerticalAlignment.CENTER

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