package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.completionrate.payload.response.CheckImportResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
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
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
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
    private val processMasterRepository: ProcessMasterRepository,
    private val productRepository: ProductRepository
) {
    val checkImportCompletionRateDate = CommonUtils.getMessage("check.importCompletionRateDate")
    val checkValidateExcelImportSuccess = CommonUtils.getMessage("validate.excel.importSuccess")
    val importSuccessMessageKey = "import.success"
    val importInsertNoData = "import.insertNoData"
    val importFileEmptyMessage = CommonUtils.getMessage("import.file.empty")
    val validateExcelInvalidFormat = CommonUtils.getMessage("validate.excel.invalidFormat")
    val validateExcelCompletionRateExdate = CommonUtils.getMessage("validate.excel.completion.rate.exdate")
    val validateExcelCompletionRateFormatError = CommonUtils.getMessage("validate.excel.completion.rate.format.error")
    val userDir = "user.dir"
    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importCompletionRateTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }


    fun checkImportExcel(file: MultipartFile, effectiveDate: OffsetDateTime, typeOfCompletionRate: Int): BaseResponse<CheckImportResponse> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(importFileEmptyMessage)
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(importFileEmptyMessage)
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate = OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(validateExcelInvalidFormat)
        val productKeys = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        var minDate: OffsetDateTime? = null
        if (typeOfCompletionRate == 0) {
            val productExists = completionRateProductRepository.getByProduct(productKeys)
            if (!productExists.isNullOrEmpty()) {
                minDate = productExists.filter { it.effectiveDate != null }
                    .minByOrNull { it.effectiveDate!! }?.effectiveDate!!
            }
        } else if (typeOfCompletionRate == 1) {
            val processExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productKeys)
            if (!processExists.isNullOrEmpty()) {
                minDate = processExists.filter { it.effectiveDate != null }
                    .minByOrNull { it.effectiveDate!! }?.effectiveDate!!
            }
        } else {
            val processProductExist = completionRateProcessProductRepository.getListProcessProductByKey(productKeys)
            if (!processProductExist.isNullOrEmpty()) {
                minDate = processProductExist.filter { it.effectiveDate != null }
                    .minByOrNull { it.effectiveDate!! }?.effectiveDate!!
            }
        }
        if (minDate != null) {
            if (minDate < currentDate && effectiveDate < currentDate && minDate > effectiveDate) {
                val formattedDate = DateTimeHelper.convertOffSetDateTimeUtc7ToString(minDate)

                return BaseResponse(CheckImportResponse(true,CommonUtils.getMessage("message.completion.error", arrayOf(formattedDate.toString()))), "")
            }
        }
        return BaseResponse(CheckImportResponse(false,""), "")
    }

    //Service Product

    fun exportCompletionRateProductExcel(search: String?, pageable: Pageable): BaseResponse<FileContentModel> {
        val products = completionRateProductRepository.getPaginatedCompletionRateProduct(search, pageable)

        val fileTemplate = File("${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, item.productName)
                ExcelHelper.setCellValue(dataRow, 1, style, item.rate.toString())
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportCompletionRateProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
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

    fun importExcelCompletionRateProduct(file: MultipartFile, effectiveDate: OffsetDateTime): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(importFileEmptyMessage)
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(importFileEmptyMessage)
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate = OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val convertEffectiveDate = DateTimeHelper.toTimeZone7(effectiveDate)
        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(validateExcelInvalidFormat)

        val productMaster = productRepository.getListNameProduct()
        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProductRepository.getByProduct(productNames)

        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists?.find { x -> x.productName == name }
            val productMasterExist = productMaster.find { x -> x == name }
            val productExistMinEffectiveDate = productExists
                ?.filter { it.productName == name }
                ?.minByOrNull { it.effectiveDate!! }

            if (productMasterExist == null) {
                errorMessages.add(CommonUtils.getMessage("product.not.exist"))
            }

            if (productExist == null) {
                if (convertEffectiveDate != null) {
                    if (convertEffectiveDate < currentDate) {
                        errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                    }
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
                errorMessages.add(validateExcelCompletionRateFormatError)
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
                    else {
                        val productExistSameDate =
                            productExists.find { x -> (x.productName == name && x.effectiveDate?.toLocalDate() == convertEffectiveDate?.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProductRepository.update(productExistSameDate)
                            count++
                        } else if (convertEffectiveDate != null) {
                            if (currentDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                val minEffectiveDate = productExistMinEffectiveDate?.effectiveDate
                                if (minEffectiveDate != null) {
                                    if (minEffectiveDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                        val compleRateProduct = CompletionRateProduct(
                                            productName = name,
                                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                            effectiveDate = effectiveDate,
                                            expirationDate = minEffectiveDate.minusDays(1)
                                        )
                                        completionRateProductRepository.add(compleRateProduct)
                                        count++
                                    }
                                    else{
                                        errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                                    }
                                }
                            } else {
                                val completionRateUpdate =
                                    completionRateProductRepository.getCompletionRateProductWithMaxEffectivedateByName(name)
                                if (completionRateUpdate != null) {
                                    if (convertEffectiveDate.toLocalDate() < completionRateUpdate.effectiveDate?.toLocalDate()) {
                                        errorMessages.add(checkImportCompletionRateDate)
                                    } else {
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
                    }
                    if (errorMessages.isEmpty()) {
                        errorMessages.add(checkValidateExcelImportSuccess)
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                }
            }

            val result = errorMessages.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
        }
        if (count == total + 1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1)))
        }
        val resultRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) == checkValidateExcelImportSuccess }
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
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage(importInsertNoData) else CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1))
        )
    }


    // Service Process

    fun exportCompletionRateProcessExcel(search: String?, pageable: Pageable): BaseResponse<FileContentModel> {
        val products = completionRateProcessRepository.getPaginatedCompletionRateProcesses(search, pageable)

        val fileTemplate = File("${System.getProperty(userDir)}/target/classes/assets/template/ExportCompletionRateProcessTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, item.processCode)
                ExcelHelper.setCellValue(dataRow, 1, style, item.processName)
                ExcelHelper.setCellValue(dataRow, 2, style, item.processNameJp)
                ExcelHelper.setCellValue(dataRow, 3, style, item.layerCode)
                ExcelHelper.setCellValue(dataRow, 4, style, "${item.rate.toString()}%")
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportCompletionRateProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
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
                    processNameJp = item.processNameJp
                )
            }, result.second
        )
    }

    fun importExcelCompletionRateProcess(file: MultipartFile, effectiveDate: OffsetDateTime): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(importFileEmptyMessage)
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(importFileEmptyMessage)
        val headerRow = sheet.getRow(0)

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        val convertEffectiveDate = DateTimeHelper.toTimeZone7(effectiveDate)
        val templateUrl = "${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(validateExcelInvalidFormat)

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate = OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val processCodeExist = processMasterRepository.getListProcessCode()

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = StringHelper.removeDecimalSuffix(ExcelHelper.getCellValue(row, 0))
            val processExistMinEffectiveDate = productExists
                ?.filter { it.key == key }
                ?.minByOrNull { it.effectiveDate!! }
            val errorMessages = mutableListOf<String>()

            val productExist = productExists?.find { x -> x.key == key }

            val processExist = processCodeExist.find { x -> x == key.take(6) }

            if (processExist == null) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.processcode"))
            }
            if (productExist == null && convertEffectiveDate != null && convertEffectiveDate < currentDate) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
            }
            if (key.length != 7) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.key.process.product"))
            }
            try {
                val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                if (rate.scale() > 2) {
                    errorMessages.add(validateExcelCompletionRateFormatError)
                }
            } catch (e: NumberFormatException) {
                errorMessages.add(validateExcelCompletionRateFormatError)
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
                    } else {
                        val productExistSameDate = productExists.find { x -> (x.key == key && x.effectiveDate?.toLocalDate() == convertEffectiveDate?.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProcessRepository.update(productExist)
                            count++

                        } else if (convertEffectiveDate != null) {
                            if (currentDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                val minEffectiveDate = processExistMinEffectiveDate?.effectiveDate
                                if (minEffectiveDate != null) {
                                    if (minEffectiveDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                        val compleRateProduct = CompletionRateProcess(
                                            key = key,
                                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                            processCode = key.take(6),
                                            layerCode = key.substring(6, 7),
                                            expirationDate = minEffectiveDate.minusDays(1),
                                            effectiveDate = effectiveDate
                                        )
                                        completionRateProcessRepository.add(compleRateProduct)
                                        count++
                                    }else{
                                        errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                                    }
                                }
                            } else {
                                val completionRateUpdate =
                                    completionRateProcessRepository.getCompletionRateProcessWithMaxEffectivedateByName(key)

                                if (completionRateUpdate != null) {
                                    if (convertEffectiveDate.toLocalDate() < completionRateUpdate.effectiveDate?.toLocalDate()) {
                                        errorMessages.add(checkImportCompletionRateDate)
                                    } else {
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
                    }
                    if (errorMessages.isEmpty()) {
                        errorMessages.add(checkValidateExcelImportSuccess)
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.product.process.error"))
                }
            }
            val result = errorMessages.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
        }
        if (count == total + 1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1)))
        }
        val resultRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) == checkValidateExcelImportSuccess }
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
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage(importInsertNoData) else CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1))
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

    fun importExcelProcessProduct(file: MultipartFile, effectiveDate: OffsetDateTime): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(importFileEmptyMessage)
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(importFileEmptyMessage)
        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty(userDir)}/target/classes/assets/template/ExportCompleteRate.xlsx"
        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 2))
            throw BusinessException(validateExcelInvalidFormat)
        val productKeys = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessProductRepository.getListProcessProductByKey(productKeys)
        val processCodeExist = processMasterRepository.getListProcessCode()
        val utcOffset = ZoneOffset.ofHours(7)
        val currentDate = OffsetDateTime.now(utcOffset).withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val convertEffectiveDate = DateTimeHelper.toTimeZone7(effectiveDate)
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()
            val productExist = productExists?.find { x -> x.key == key }
            val processExist = processCodeExist.find { x -> x == key.take(6) }
            val processProductExistMinEffectiveDate = productExists
                ?.filter { it.key == key }
                ?.minByOrNull { it.effectiveDate!! }
            if (processExist == null) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.processcode"))
            }
            if (productExist == null && convertEffectiveDate != null && convertEffectiveDate < currentDate) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
            }

            if (key.length != 14) {
                errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.key.process"))
            }
            try {
                val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                if (rate.scale() > 2) {
                    errorMessages.add(validateExcelCompletionRateFormatError)
                }
            } catch (e: NumberFormatException) {
                errorMessages.add(validateExcelCompletionRateFormatError)
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
                    } else {
                        val productExistSameDate =
                            productExists.find { x -> (x.key == key && x.effectiveDate?.toLocalDate() == convertEffectiveDate?.toLocalDate()) }
                        if (productExistSameDate != null) {
                            productExistSameDate.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                            completionRateProcessProductRepository.update(productExistSameDate)
                            count++
                        } else if (convertEffectiveDate != null) {
                            if (currentDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                val minEffectiveDate = processProductExistMinEffectiveDate?.effectiveDate
                                if (minEffectiveDate != null) {
                                    if (minEffectiveDate.toLocalDate() > convertEffectiveDate.toLocalDate()) {
                                        val compleRateProcessProduct = CompletionRateProcessProduct(
                                            key = key,
                                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1)),
                                            productNameShortcut = key.substring(6, 13),
                                            processCode = key.take(6),
                                            layerCode = key.substring(13, 14),
                                            expirationDate = minEffectiveDate.minusDays(1),
                                            effectiveDate = effectiveDate
                                        )
                                        completionRateProcessProductRepository.add(compleRateProcessProduct)
                                        count++
                                    }else{
                                        errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.exdate"))
                                    }
                                }
                            } else {
                                val completionRateUpdate =
                                    completionRateProcessProductRepository.getCompletionRateProcessProductWithMaxEffectivedateByName(key)
                                if (completionRateUpdate != null) {
                                    if (effectiveDate.toLocalDate() < completionRateUpdate.effectiveDate?.toLocalDate()) {
                                        errorMessages.add(checkImportCompletionRateDate)
                                    } else {
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
                    }
                    if (errorMessages.isEmpty()) {
                        errorMessages.add(checkValidateExcelImportSuccess)
                    }
                } catch (e: Exception) {
                    errorMessages.add(CommonUtils.getMessage("validate.excel.completion.rate.process.error"))
                }
            }

            val result = errorMessages.joinToString(separator = "; ")
            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
        }
        if (count == total + 1) {
            workbook.close()
            return BaseResponse(null, CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1)))
        }
        val resultRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) == checkValidateExcelImportSuccess }
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
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )
        workbook.close()
        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage(importInsertNoData) else CommonUtils.getMessage(importSuccessMessageKey, arrayOf(count, total + 1))
        )
    }

    fun exportCompletionRateProcessProductExcel(search: CompletionRateProcessProductRequest?, pageable: Pageable): BaseResponse<FileContentModel> {
        val processproducts = completionRateProcessProductRepository.getPaginatedCompletionRateProcessesProduct(search, pageable)

        val fileTemplate = File("${System.getProperty(userDir)}/target/classes/assets/template/ExportCompletionProcessProductRateTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (processproducts.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in processproducts.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, item.key)
                ExcelHelper.setCellValue(dataRow, 1, style, item.processCode)
                ExcelHelper.setCellValue(dataRow, 2, style, item.processName)
                ExcelHelper.setCellValue(dataRow, 3, style, item.processNameJp)
                ExcelHelper.setCellValue(dataRow, 4, style, item.layerCode)
                ExcelHelper.setCellValue(dataRow, 5, style, "${item.rate.toString()}%")
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportCompletionRateProductProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}