package com.kcvn.spm.app.inventoryproduct.completionrate.service

import com.kcvn.spm.app.inventoryproduct.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.inventoryproduct.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.inventoryproduct.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.app.inventoryproduct.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcess
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional
class CompletionRateService(
    private val completionRateProductRepository: CompletionRateProductRepository,
    private val completionRateProcessProductRepository: CompletionRateProcessProductRepository,
    private val completionRateProcessRepository: CompletionRateProcessRepository,
    private val processMasterRepository : ProcessMasterRepository,
    private val productRepository : ProductRepository
) {

    val completionRateResultKey = CommonUtils.getMessage("validate.excel.complition.rate.result")
    val completionValidateFormatError = CommonUtils.getMessage("validate.excel.complition.rate.format.error")
    val completionRateFileEmpty = CommonUtils.getMessage("import.file.empty")
    val completionRateFileWrongFormat = CommonUtils.getMessage("validate.excel.invalidFormat")
    val keyRate = "TLD(rate)"
    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "ImportCompletionTemplate.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        return BaseResponse(response)
    }


    //Service Product

    fun exportCompletionRateProductExcel(search: String?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = completionRateProductRepository.findByKeywordPaginated(search, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = "Times New Roman"
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)


            var rowNumber = 1
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.productName)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.rate.toString())
                dataRow.getCell(1).cellStyle = style


            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.file.completion.rate.product"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun getPaginatedCompletionRateProduct(search: String?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProductRepository.getPaginatedCompletionRateProduct(search, pageable)
        return PaginatedResponse(
            data = result.first.map { item ->
                CompletionRateProductResponse(
                    id = item.id,
                    productName = item.productName,
                    rate = item.rate,

                    )

            }, result.second
        )
    }

    fun importExcelCompletionRateProduct(file: MultipartFile,effectiveDate: OffsetDateTime) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate =OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val lastRowIndex = sheet.lastRowNum
        if (lastRowIndex < 1) {
            throw BusinessException(completionRateFileEmpty)
        }else{
            val firstRow = sheet.getRow(0)
            val cellAValue = firstRow.getCell(0)?.stringCellValue
            val cellBValue = firstRow.getCell(1)?.stringCellValue

            if (cellAValue != "Key" || cellBValue != keyRate) {
                throw BusinessException(completionRateFileWrongFormat)
            }

        }
//        if (!sheet.any { x -> x.rowNum > rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productMaster = productRepository.getListNameProduct();
        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProductRepository.getByProduct(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == completionRateResultKey
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue("Kết quả")
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(headerCell, 15000)

        }

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.productName == name }
            val productMasterExist = productMaster.find {x -> x == name}

            if(productMasterExist == null){
                errorMessages.add(CommonUtils.getMessage("product.not.exist"))

            }
            if (effectiveDate <= currentDate) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.exdate"))

            }
            if (productExist == null) {
                if (name.length != 12) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.product.key"))
                }

                try {
                    val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                    if (rate.scale() > 2) {
                        errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.product.rate"))
                    }
                } catch (e: NumberFormatException) {
                    errorMessages.add(completionValidateFormatError)
                }
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProduct = CompletionRateProduct(
                            productName = name,
                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            effectiveDate = effectiveDate,
                            expirationDate = null

                        )

                        completionRateProductRepository.add(compleRateProduct)
                    } else {
                        productExist.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                        productExist.effectiveDate = effectiveDate
                        productExist.expirationDate = effectiveDate.minusDays(1)
                        completionRateProductRepository.update(productExist)
                    }
                    errorMessages.add("OK")
                    count++
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.product.error"))
                }
            }

            val result = errorMessages.joinToString(separator = "; ")
            if (!checkColResult) {
                row.createCell(row.lastCellNum + 0).setCellValue(result)
                row.getCell(row.lastCellNum - 1).cellStyle = style
            } else {
                row.getCell(row.lastCellNum - 1).setCellValue(result)
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("import.file.completion.rate.product"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total+1))
        )
    }


    // Service Process

    fun exportCompletionRateProcessExcel(search: String?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = completionRateProcessRepository.getPaginatedCompletionRateProcesses(search, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompletionRateProcessTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = "Times New Roman"
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)


            var rowNumber = 1
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.processCode)
                dataRow.getCell(0).cellStyle = style


                dataRow.createCell(1).setCellValue(item.processName)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.processNameJp)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.layerCode)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.rate.toString()+"%")
                dataRow.getCell(4).cellStyle = style


            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.file.completion.rate.process"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun getPaginatedCompletionRateProcesses(search: String?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProcessRepository.getPaginatedCompletionRateProcesses(search, pageable)
        return PaginatedResponse(
            data = result.first.map { item ->
                CompletionRateProcessResponse(
                    id = item.id,
                    key = item.key,
                    processCode = item.processCode,
                    layerCode = item.layerCode,
                    rate = item.rate,
                    processName = item.processName,
                    processNameJp =item.processNameJp
                    )
            }, result.second
        )
    }

    fun importExcelCompletionRateProcess(file: MultipartFile, effectiveDate: OffsetDateTime) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)

        val rowIndex = 1
        val lastRowIndex = sheet.lastRowNum
        if (lastRowIndex < 1) {
            throw BusinessException(completionRateFileEmpty)
        }else{
            val firstRow = sheet.getRow(0)
            val cellAValue = firstRow.getCell(0)?.stringCellValue
            val cellBValue = firstRow.getCell(1)?.stringCellValue

            if (cellAValue != "Key" || cellBValue != keyRate) {
                throw BusinessException(completionRateFileWrongFormat)
            }

        }
//        if (!sheet.any { x -> x.rowNum > rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate =OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == completionRateResultKey
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue(completionRateResultKey)
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(headerCell, 15000)
        }

        val processCodeExist = processMasterRepository.getListProcessCode()

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.key == key }

            val processExist = processCodeExist.find { x -> x == key.take(6) }
            if (effectiveDate <= currentDate) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.exdate"))

            }
            if(processExist == null){
                errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.processcode"))
            }
            if (productExist == null) {
                if (key.length != 7) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.key.process.product"))
                }

                try {
                    val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                    if (rate.scale() > 2) {
                        errorMessages.add(completionValidateFormatError)
                    }
                } catch (e: NumberFormatException) {
                    errorMessages.add(completionValidateFormatError)
                }
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProduct = CompletionRateProcess(
                            key = key,
                            rate =  BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            processCode = key.take(6),
                            layerCode = key.substring(6, 7),
                            expirationDate = null,
                            effectiveDate = effectiveDate
                        )

                        completionRateProcessRepository.add(compleRateProduct)
                    } else {
                        productExist.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                        productExist.effectiveDate = effectiveDate
                        productExist.expirationDate = effectiveDate.minusDays(1)
                        completionRateProcessRepository.update(productExist)
                    }
                    errorMessages.add("OK")
                    count++
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.product.process.error"))
                }
            }

            val result = errorMessages.joinToString(separator = "; ")
            if (!checkColResult) {
                row.createCell(row.lastCellNum + 0).setCellValue(result)
                row.getCell(row.lastCellNum - 1).cellStyle = style
            } else {
                row.getCell(row.lastCellNum - 1).setCellValue(result)
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("import.file.completion.rate.process"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total+1))
        )
    }


    //Service Process Product
    fun getPaginatedCompletionRateProcessesProduct(search: CompletionRateProcessProductRequest?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProcessProductRepository.getPaginatedCompletionRateProcessesProduct(search, pageable)
        return PaginatedResponse(
            data = result.first.map { item ->
                CompletionRateProcessProductResponse(
                    id = item.id,
                    key = item.key,
                    productNameShortcut = item.productNameShortcut,
                    processCode = item.processCode,
                    layerCode = item.layerCode,
                    rate = item.rate,
                    processName = item.processName,
                    processNameJp = item.processNameJp

                )
            }, result.second
        )
    }

    fun importExcelProcessProduct(file: MultipartFile, effectiveDate: OffsetDateTime): BaseResponse<FileContentModel>{
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        val lastRowIndex = sheet.lastRowNum
        if (lastRowIndex < 1) {
            throw BusinessException(completionRateFileEmpty)
        } else{
            val firstRow = sheet.getRow(0)
            val cellAValue = firstRow.getCell(0)?.stringCellValue
            val cellBValue = firstRow.getCell(1)?.stringCellValue

            if (cellAValue != "Key" || cellBValue != keyRate) {
                throw BusinessException(completionRateFileWrongFormat)
            }

        }
//        if (!sheet.any { x -> x.rowNum > rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productKeys = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessProductRepository.getListProductByKey(productKeys)
        val processCodeExist = processMasterRepository.getListProcessCode()
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate =OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == completionRateResultKey
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue(completionRateResultKey)
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(headerCell, 15000)
        }

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.key == key }

            val processExist = processCodeExist.find {x -> x == key.substring(7, 13)}
            if(processExist == null){
                errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.processcode"))
            }
            if (effectiveDate <= currentDate) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.exdate"))

            }
            if (productExist == null) {
                if (key.length != 14) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.key.process"))
                }

                try {
                    val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                    if (rate.scale() > 2) {
                        errorMessages.add(completionValidateFormatError)
                    }
                } catch (e: NumberFormatException) {
                    errorMessages.add(completionValidateFormatError)
                }
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProcessProduct = CompletionRateProcessProduct(
                            key = key,
                            rate =  BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            productNameShortcut = key.take(7),
                            processCode = key.substring(7, 13),
                            layerCode = key.substring(13, 14),
                            expirationDate = null,
                            effectiveDate = effectiveDate


                        )

                        completionRateProcessProductRepository.add(compleRateProcessProduct)
                    } else {
                        productExist.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                        productExist.effectiveDate = effectiveDate

                        productExist.expirationDate = effectiveDate.minusDays(1)
                        completionRateProcessProductRepository.update(productExist)
                    }
                    errorMessages.add("OK")
                    count++
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.complition.rate.process.error"))
                }
            }

            val result = errorMessages.joinToString(separator = "; ")
            if (!checkColResult) {
                row.createCell(row.lastCellNum + 0).setCellValue(result)
                row.getCell(row.lastCellNum - 1).cellStyle = style
            } else {
                row.getCell(row.lastCellNum - 1).setCellValue(result)
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("import.file.completion.rate.process.product"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total+1))
        )
    }

    fun exportCompletionRateProcessProductExcel(search: CompletionRateProcessProductRequest?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val processproducts = completionRateProcessProductRepository.getPaginatedCompletionRateProcessesProduct(search, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompletionProcessProductRateTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (processproducts.first.isNotEmpty()) {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = "Times New Roman"
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)


            var rowNumber = 1
            for (item in processproducts.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.key)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.processCode)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.processName)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.processNameJp)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.layerCode)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item.rate.toString()+"%")
                dataRow.getCell(5).cellStyle = style


            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.file.completion.rate.process.product"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}