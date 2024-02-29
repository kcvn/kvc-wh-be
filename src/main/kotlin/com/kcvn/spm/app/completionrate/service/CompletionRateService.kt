package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.StringHelper
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class CompletionRateService(
        private val completionRateProductRepository: CompletionRateProductRepository,
        private val completionRateProcessProductRepository: CompletionRateProcessProductRepository,
        private val completionRateProcessRepository: CompletionRateProcessRepository,
        private val processMasterRepository : ProcessMasterRepository,
        private val productRepository : ProductRepository
) {
    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
                fileName = CommonUtils.getMessage("fileName.importCompletionRateTemplate"),
                contentType = Constants.EXCEL_CONTENT_TYPE,
                content = excelBytes
        )

        return BaseResponse(response)
    }


    //Service Product

    fun exportCompletionRateProductExcel(search: String?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = completionRateProductRepository.getPaginatedCompletionRateProduct(search, pageable)

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
            font.fontName = Constants.FONT_TIMES_NEW_ROMAN
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
                fileName = CommonUtils.getMessage("fileName.exportCompletionRateProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
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

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate =OffsetDateTime.now(utcOffset).withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val productMaster = productRepository.getListNameProduct()
        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProductRepository.getByProduct(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex
        val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
        val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
        val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == CommonUtils.getMessage("excel.colResultName")
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue(CommonUtils.getMessage("excel.colResultName"))
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

            if (productExist == null) {
                if (effectiveDate <= currentDate) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))

                }
                if (name.length != 12) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.product.key"))
                }
            }
            try {
                val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                if (rate.scale() > 2) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.product.rate"))
                }
            } catch (e: NumberFormatException) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.format.error"))
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
                        count++

                    }
                    // ton tai
                    else {
                        val productExistSameDate =
                            productExists.find { x -> (x.productName == name && x.effectiveDate?.toLocalDate() == effectiveDate.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProductRepository.update(productExistSameDate)
                            count++
                        } else if (currentDate.toLocalDate() > effectiveDate.toLocalDate()) {
                            errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                        } else {
                            val completionRateUpdate =
                                completionRateProductRepository.getCompletionRateProductWithMaxEffectivedateByName(name)
                            if (completionRateUpdate != null) {
                                if(effectiveDate.toLocalDate()< completionRateUpdate.effectiveDate?.toLocalDate()){
                                    errorMessages.add(CommonUtils.getMessage("check.importCompletionRateDate"))
                                }else {
                                    completionRateUpdate.expirationDate = effectiveDate.minusDays(1)
                                    completionRateProductRepository.update(completionRateUpdate)
                                    val compleRateProduct = CompletionRateProduct(
                                        productName = name,
                                        rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                        effectiveDate = effectiveDate,
                                        expirationDate = null

                                    )
                                    completionRateProductRepository.add(compleRateProduct)

                                    count++
                                }
                            }

                        }
                    }
                    if(errorMessages.isEmpty()){
                        errorMessages.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.updateDataError"))
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
        if (count == total+1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total+1)))
        }
        val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
        for (row in resultRows) {
            val rowNum = row.rowNum
            sheet.removeRow(row)
            if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
                sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
                fileName = CommonUtils.getMessage("fileName.resultImportCompletionRateProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
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
            font.fontName = Constants.FONT_TIMES_NEW_ROMAN
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
                fileName = CommonUtils.getMessage("fileName.exportCompletionRateProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
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
        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)
        val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
        val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
        val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate =OffsetDateTime.now(utcOffset).withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == CommonUtils.getMessage("excel.colResultName")
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue(CommonUtils.getMessage("excel.colResultName"))
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(headerCell, 15000)
        }

        val processCodeExist = processMasterRepository.getListProcessCode()

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 0))

            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.key == key }

            val processExist = processCodeExist.find { x -> x == key.take(6) }

            if(processExist == null){
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.processcode"))
            }
            if (productExist == null) {
                if (effectiveDate <= currentDate) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))

                }

            }
            if (key.length != 7) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.key.process.product"))
            }
            try {
                val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                if (rate.scale() > 2) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.format.error"))
                }
            } catch (e: NumberFormatException) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.format.error"))
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProduct = CompletionRateProcess(
                            key = key,
                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            processCode = key.take(6),
                            layerCode = key.substring(6, 7),
                            expirationDate = null,
                            effectiveDate = effectiveDate
                        )
                        completionRateProcessRepository.add(compleRateProduct)
                        count++
                    }
                        else{
                        val productExistSameDate =
                            productExists.find { x -> (x.key == key && x.effectiveDate?.toLocalDate() == effectiveDate.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProcessRepository.update(productExist)
                            count++

                        } else if (currentDate > effectiveDate) {
                            errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                        }
                        else{
                            val completionRateUpdate =
                                completionRateProcessRepository.getCompletionRateProcessWithMaxEffectivedateByName(key)

                            if (completionRateUpdate != null) {
                                if(effectiveDate.toLocalDate()< completionRateUpdate.effectiveDate?.toLocalDate()){
                                    errorMessages.add(CommonUtils.getMessage("check.importCompletionRateDate"))
                                }else {
                                    completionRateUpdate.expirationDate = effectiveDate.minusDays(1)
                                    completionRateProcessRepository.update(completionRateUpdate)

                                    val compleRateProduct = CompletionRateProcess(
                                        key = key,
                                        rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                        processCode = key.take(6),
                                        layerCode = key.substring(6, 7),
                                        expirationDate = null,
                                        effectiveDate = effectiveDate
                                    )
                                    completionRateProcessRepository.add(compleRateProduct)

                                    count++
                                }

                            }

                        }
                    }
                    if(errorMessages.isEmpty()){
                        errorMessages.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.product.process.error"))
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

        if (count == total+1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total+1)))
        }
        val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
        for (row in resultRows) {
            val rowNum = row.rowNum
            sheet.removeRow(row)
            if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
                sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
                fileName = CommonUtils.getMessage("fileName.resultImportCompletionRateProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
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
                            processNameJp = item.processNameJp,
                            effectiveDate = item.effectiveDate

                    )
                }, result.second
        )
    }

    fun importExcelProcessProduct(file: MultipartFile, effectiveDate: OffsetDateTime): BaseResponse<FileContentModel>{
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

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
        val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
        val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
        val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == CommonUtils.getMessage("excel.colResultName")
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue(CommonUtils.getMessage("excel.colResultName"))
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            sheet.setColumnWidth(headerCell, 15000)
        }

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.key == key }

            val processExist = processCodeExist.find {x -> x ==  key.take(6)}
            if(processExist == null){
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.processcode"))
            }

            if (productExist == null) {
                if (effectiveDate <= currentDate) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))

                }
            }
            if (key.length != 14) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.key.process"))
            }

            try {
                val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                if (rate.scale() > 2) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.format.error"))
                }
            } catch (e: NumberFormatException) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.format.error"))
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProcessProduct = CompletionRateProcessProduct(
                            key = key,
                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            productNameShortcut = key.substring(6, 13),
                            processCode = key.take(6),
                            layerCode = key.substring(13, 14),
                            expirationDate = null,
                            effectiveDate = effectiveDate


                        )
                        completionRateProcessProductRepository.add(compleRateProcessProduct)
                        count++
                    }
                        else{
                        val productExistSameDate =
                            productExists.find { x -> (x.key == key && x.effectiveDate?.toLocalDate() == effectiveDate.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProcessProductRepository.update(productExistSameDate)
                            count++
                        } else if (currentDate > effectiveDate) {
                            errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                        }
                        else{
                            val completionRateUpdate =
                                completionRateProcessProductRepository.getCompletionRateProcessProductWithMaxEffectivedateByName(key)
                            if (completionRateUpdate != null ) {
                                if(effectiveDate.toLocalDate()< completionRateUpdate.effectiveDate?.toLocalDate()){
                                    errorMessages.add(CommonUtils.getMessage("check.importCompletionRateDate"))
                                }else{
                                    completionRateUpdate.expirationDate = effectiveDate.minusDays(1)
                                    completionRateProcessProductRepository.update(completionRateUpdate)
                                    val compleRateProcessProduct = CompletionRateProcessProduct(
                                        key = key,
                                        rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                        productNameShortcut = key.substring(6, 13),
                                        processCode = key.take(6),
                                        layerCode = key.substring(13, 14),
                                        expirationDate = null,
                                        effectiveDate = effectiveDate
                                    )
                                    completionRateProcessProductRepository.add(compleRateProcessProduct)

                                    count++
                                }

                            }


                        }


                    }
                    if(errorMessages.isEmpty()){
                        errorMessages.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.process.error"))
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
        if (count == total+1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total+1)))
        }
        val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
        for (row in resultRows) {
            val rowNum = row.rowNum
            sheet.removeRow(row)
            if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
                sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
                fileName = CommonUtils.getMessage("fileName.resultImportCompletionRateProductProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
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
            font.fontName = Constants.FONT_TIMES_NEW_ROMAN
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
                fileName = CommonUtils.getMessage("fileName.exportCompletionRateProductProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
                content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}