package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.model.ImportInventoryErrorModel
import com.kcvn.spm.app.inventoryproduct.payload.request.InventoryProductRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper.Companion.convertOffSetDateTimeUtc7ToString
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.CellStyleModel
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@Service
@Transactional
class InventoryProductService(
    private val inventoryProductRepository: InventoryProductRepository,
    private val processProcedureRep: ProcessProcedureStructureRepository

) {
    fun checkInventoryDate(date: OffsetDateTime): CheckInventoryDateResponse? {
        val data = CheckInventoryDateResponse()
        val query = inventoryProductRepository.findDateInventoryProduct(date)
        if (query != null) {
            data.hasInventoryDate = true
            data.inventorydate = query.inventoryDate
            return data
        }
        return data
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importInventoryTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExelInventoryProduct(date: OffsetDateTime, file: MultipartFile): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        var count = 0
        val total = sheet.lastRowNum
        val headerRow = sheet.getRow(0)
        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryProductTemplate.xlsx"
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 17))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val requestDelete = InventoryProduct()
        requestDelete.inventoryDate = date
        inventoryProductRepository.deleteInventoryProduct(requestDelete)
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            if (checkIsEmptyRow(row)) {
                continue
            }
            val style = row.getCell(0).cellStyle
            val messageResults = mutableListOf<String>()
            var check = true
            if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 6).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 6))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 7).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 7))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 9).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 9))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 11).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 11))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 2).isNotEmpty() && row.getCell(2).toString().length > 8) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 11))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 3).isNotEmpty() && row.getCell(3).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3), 50)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 5).isNotEmpty() && row.getCell(5).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5), 50)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 6).isNotEmpty() && row.getCell(6).toString().length > 4) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 6), 2)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 7).isNotEmpty() && row.getCell(7).toString().length > 100) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 7), 100)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 9).isNotEmpty() && row.getCell(9).toString().length > 12) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 9), 12)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 11).isNotEmpty() && row.getCell(11).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 11), 50)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 14).isNotEmpty() && row.getCell(14).cellType != CellType.NUMERIC) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 14), 100)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 15).isNotEmpty() && row.getCell(15).cellType != CellType.NUMERIC) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 15), 100)
                    )
                )
            }

            try {
                if (check) {
                    val cellProcessCode = row.getCell(2)

                    val processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                        cellProcessCode.numericCellValue.toInt().toString()
                    else {
                        ExcelHelper.getCellValue(row, 2)
                    }

                    val cellLayerCode = row.getCell(6)

                    val layerCode = if (cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0)
                        cellLayerCode.numericCellValue.toInt().toString()
                    else {
                        ExcelHelper.getCellValue(row, 6)
                    }
                    val filter = ImportProcessRequest(
                        productName = ExcelHelper.getCellValue(row, 9),
                        processCode = processCode,
                        layerCode = layerCode
                    )
                    val filterCheckProcessProcedure = processProcedureRep.getByFilterProcessStructureByInventoryProduct(filter)
                    if (filterCheckProcessProcedure == null) {
                        messageResults.add(CommonUtils.getMessage("validate.excel.inventoryProduct.dataNull"))
                    } else {
                        val requestImport = InventoryProduct(
                            //new field
                            employeeCode = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 0)),
                            team = ExcelHelper.getCellValue(row, 1),
                            processNameJp = ExcelHelper.getCellValue(row, 4),
                            processingDirective = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 8)).toIntOrNull(),
                            piecesPerSheet = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 10)).toIntOrNull(),
                            productionAreaName = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 12)),
                            processCount = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 13)).toIntOrNull(),
                            seidenRepNumber = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 16)).toIntOrNull(),
                            processName =  ExcelHelper.getCellValue(row, 3),
                            //end
                            processProcedureStructureId = filterCheckProcessProcedure.id,
                            inventoryDate = date,
                            code = if (ExcelHelper.getCellValue(row, 5).toBigDecimalOrNull() != null) {
                                ExcelHelper.getCellValue(row, 5).toBigDecimalOrNull()?.toLong().toString()
                            } else {
                                ExcelHelper.getCellValue(row, 5)
                            },
                            tapeLotNo = ExcelHelper.getCellValue(row, 7),
                            orderCode = ExcelHelper.getCellValue(row, 11),
                            productQuantity = ExcelHelper.getCellValue(row, 14).toBigDecimalOrNull()?.toInt(),
                            sheetQuantity = ExcelHelper.getCellValue(row, 15).toBigDecimalOrNull()?.toInt()
                        )
                        val checkInventoryProduct = inventoryProductRepository.findInventoryProduct(filterCheckProcessProcedure.id, date, requestImport.code ?: "")

                        if (checkInventoryProduct == null) {
                            requestImport.createdDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            requestImport.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                            inventoryProductRepository.insertInventoryProduct(requestImport)
                        } else {
                            requestImport.updatedBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                            requestImport.updatedDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            inventoryProductRepository.updateInventoryProduct(requestImport)
                        }
                        messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                        count++
                    }
                }
            } catch (e: Exception) {
                messageResults.add(CommonUtils.getMessage("validate.excel.inventoryProduct.data.update.err"))
            }
            val result = messageResults.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
        }

        if (count == total) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))
        }

        val resultRows = sheet.filter {
            x -> ExcelHelper.getCellValue(x, colIndexResult) != CommonUtils.getMessage("validate.excel.importSuccess")
            && x.rowNum >= rowIndex
        }.map { x ->
            ImportInventoryErrorModel(
                //new field
                employeeCode = ExcelHelper.getCellValue(x, 0),
                team = ExcelHelper.getCellValue(x, 1),
                processNameJp = ExcelHelper.getCellValue(x, 4),
                processingDirective = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(x, 8)).toIntOrNull(),
                piecesPerSheet = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(x, 10)).toIntOrNull(),
                productionAreaName = ExcelHelper.getCellValue(x, 12),
                processCount = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(x, 13)).toIntOrNull(),
                seidenRepNumber = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(x, 16)).toIntOrNull(),

                //end

                processCode = ExcelHelper.getCellValue(x, 2),
                processName = ExcelHelper.getCellValue(x, 3),
                code = if (ExcelHelper.getCellValue(x, 5).toBigDecimalOrNull() != null) {
                    ExcelHelper.getCellValue(x, 5).toBigDecimalOrNull()?.toLong().toString()
                } else {
                    ExcelHelper.getCellValue(x, 5)
                },
                layerCode = ExcelHelper.getCellValue(x, 6),
                tapeLotNo = ExcelHelper.getCellValue(x, 7),
                productName = ExcelHelper.getCellValue(x, 9),
                orderCode = ExcelHelper.getCellValue(x, 11),
                productQuantity = ExcelHelper.getCellValue(x, 14).toBigDecimalOrNull()?.toInt(),
                sheetQuantity = ExcelHelper.getCellValue(x, 15).toBigDecimalOrNull()?.toInt(),
                messageError = ExcelHelper.getCellValue(x, colIndexResult),
                cellStyles = x.map { m -> CellStyleModel(m.columnIndex, m.cellStyle) }
            )
        }

        val response = exportErrorFile(resultRows, headerRow, workbook, sheet)

        workbook.close()

        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }


    fun checkIsEmptyRow( row: Row): Boolean {
        for (i in 0 until row.lastCellNum) {
            if (row.getCell(i) != null && row.getCell(i).cellType != CellType.BLANK) {
                return false
            }
        }
        return true

    }

    private fun exportErrorFile(inventories: List<ImportInventoryErrorModel>, titleRow: Row, workbook: Workbook, importSheet: Sheet): FileContentModel {
        val sheet = workbook.createSheet()
        val headerRow: Row = sheet.getRow(0) ?: sheet.createRow(0)
        headerRow.height = titleRow.height

        for (i in 0 until titleRow.lastCellNum) {
            val headerStyle = titleRow.getCell(i).cellStyle
            val headerCellValue = ExcelHelper.getCellValue(titleRow, i)
            ExcelHelper.setCellValue(headerRow, i, headerStyle, headerCellValue)
            sheet.setColumnWidth(i, importSheet.getColumnWidth(i))
        }

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
        val rowHeight = importSheet.getRow(1).height
        var rowNumber = 1
        for (item in inventories) {
            val dataRow: Row = sheet.createRow(rowNumber)
            dataRow.height = rowHeight
            item.cellStyles.find { x -> x.index == 0 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 0, style.cellStyle,
                    item.employeeCode?.let { StringHelper.removeDecimalSuffix(it) })
            }
            item.cellStyles.find { x -> x.index == 1 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 1, style.cellStyle, item.team)
            }
            item.cellStyles.find { x -> x.index == 4 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 4, style.cellStyle, item.processNameJp)
            }
            item.cellStyles.find { x -> x.index == 8 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 8, style.cellStyle, item.processingDirective.toString())
            }
            item.cellStyles.find { x -> x.index == 10 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 10, style.cellStyle, if(item.piecesPerSheet == null) "" else item.piecesPerSheet.toString())
            }
            item.cellStyles.find { x -> x.index == 12 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 12, style.cellStyle,
                    item.productionAreaName?.let { StringHelper.removeDecimalSuffix(it) })
            }
            item.cellStyles.find { x -> x.index == 13 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 13, style.cellStyle, if(item.processCount == null) "" else item.processCount.toString())
            }
            item.cellStyles.find { x -> x.index == 16 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 16, style.cellStyle, if(item.seidenRepNumber == null) "" else item.seidenRepNumber.toString())
            }
            item.cellStyles.find { x -> x.index == 2 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 2, style.cellStyle,
                    item.processCode?.let { StringHelper.removeDecimalSuffix(it) })
            }
            item.cellStyles.find { x -> x.index == 3 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 3, style.cellStyle, item.processName)
            }

            item.cellStyles.find { x -> x.index == 5 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 5, style.cellStyle, item.code?.toBigDecimalOrNull()?.toLong().toString())
            }
            item.cellStyles.find { x -> x.index == 6 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 6, style.cellStyle,
                    item.layerCode?.let { StringHelper.removeDecimalSuffix(it) })
            }
            item.cellStyles.find { x -> x.index == 7 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 7, style.cellStyle, item.tapeLotNo)
            }
            item.cellStyles.find { x -> x.index == 9 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 9, style.cellStyle, item.productName)
            }
            item.cellStyles.find { x -> x.index == 11 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 11, style.cellStyle, item.orderCode)
            }
            item.cellStyles.find { x -> x.index == 14 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 14, style.cellStyle, item.productQuantity?.toString())
            }
            item.cellStyles.find { x -> x.index == 15 }?.let { style ->
                ExcelHelper.setCellValue(dataRow, 15, style.cellStyle, item.sheetQuantity?.toString())
            }

            ExcelHelper.setCellValue(dataRow, colIndexResult, item.cellStyles.find { x -> x.index == colIndexResult }!!.cellStyle, item.messageError)
            rowNumber++
        }

        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)


        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportInventory", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return response
    }

    fun getListInventoryProduct(request: InventoryProductRequest?, pageable: Pageable): BasePagingResponse<InventoryProductResponse?> {
        val result = inventoryProductRepository.findByKeywordPaginated(request, pageable)
        val response = BasePagingResponse<InventoryProductResponse?>()
        response.data = result.first.map { inventoryProduct ->
            InventoryProductResponse(
                inventoryDate = inventoryProduct?.inventoryDate,
                productQuantity = inventoryProduct?.productQuantity,
                sheetQuantity = inventoryProduct?.sheetQuantity,
                orderCode = inventoryProduct?.orderCode,
                tapeLotNo = inventoryProduct?.tapeLotNo,
                processCode = inventoryProduct?.processCode,
                productName = inventoryProduct?.productName,
                processName = inventoryProduct?.processName,
                code = inventoryProduct?.code,
                pcsSh = inventoryProduct?.pcsSh,
                layerCode = inventoryProduct?.layerCode,
                team = inventoryProduct?.team,
                processNameJp = inventoryProduct?.processNameJp,
                processingDirective = inventoryProduct?.processingDirective,
                piecesPerSheet = inventoryProduct?.piecesPerSheet,
                productionAreaName = inventoryProduct?.productionAreaName,
                processCount = inventoryProduct?.processCount,
                seidenRepNumber = inventoryProduct?.seidenRepNumber
            )
        }
        response.totalRecords = result.second ?: 0
        return response
    }

    fun exportExcel(request: InventoryProductRequest?, pageable: Pageable): BaseResponse<FileContentModel> {
        val inventoryProduct = inventoryProductRepository.findByKeywordPaginated(request, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportInventoryProductTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (inventoryProduct.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in inventoryProduct.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                if (item?.inventoryDate != null) {
                    val formattedDate = convertOffSetDateTimeUtc7ToString(item.inventoryDate!!)
                    ExcelHelper.setCellValueCustom(workbook,dataRow, 0, style, formattedDate, isAlignCenter = true)
                }
                ExcelHelper.setCellValueCustom(workbook,dataRow, 1, style, item?.employeeCode, isAlignCenter = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 2, style, item?.team)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 3, style, item?.processCode, isAlignCenter = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 4, style, item?.processName)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 5, style, item?.processNameJp)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 6, style, item?.code, isAlignCenter = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 7, style, item?.layerCode,isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 8, style, item?.tapeLotNo)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 9, style, item?.processingDirective?.toString() ?: "",isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 10, style, item?.productName)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 11, style, item?.piecesPerSheet?.toString() ?: "",isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 12, style, item?.orderCode, isAlignCenter = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 13, style, item?.productionAreaName)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 14, style, item?.processCount?.toString() ?: "",isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 15, style, item?.productQuantity?.toString() ?: "",isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 16, style, item?.sheetQuantity?.toString() ?: "",isAlignRight = true)
                ExcelHelper.setCellValueCustom(workbook,dataRow, 17, style, item?.seidenRepNumber?.toString() ?: "",isAlignRight = true)
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.inventoryProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}

