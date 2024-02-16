package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.payload.model.LayerImportCompletionRateProductModel
import com.kcvn.spm.app.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.app.product.payload.model.LayerImportProductModel
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.helper.jsonhelper.JsonConvert
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcess
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.repository.CompletionRateProcessRepository
import com.kcvn.spm.repository.CompletionRateProductRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jooq.tools.csv.CSVReader
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Service
@Transactional
class CompletionRateService(
    private val completionRateProductRepository: CompletionRateProductRepository,
    private val completionRateProcessProductRepository: CompletionRateProcessProductRepository,
    private val completionRateProcessRepository: CompletionRateProcessRepository
) {

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
            fileName = "Danh_sach_ti_le_dat_san_pham.xlsx",
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

    fun importCsvCompletionRateProduct(file: MultipartFile): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = data.mapNotNull { x -> x[0] }

        //wong here
        val productExists = completionRateProductRepository.getByProduct(productNames)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.productName == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProduct(
                        productName = item[0],
                        rate =  BigDecimal(item[1])
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                        layers.add(layer)
                    }
                    completionRateProductRepository.add(compleRateProduct)
                } else {
                    productExist.productName = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProductRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }

    fun importExcelCompletionRateProduct(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProductRepository.getByProduct(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == "Kết quả"
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue("Kết quả")
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            headerRow.getCell(headerCell).cellStyle.fillForegroundColor = IndexedColors.RED.index
            headerRow.getCell(headerCell).cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            sheet.setColumnWidth(headerCell, 15000)
        }

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.productName == name }

            if (productExist == null) {
                if (name.length != 12) {
                    errorMessages.add("Tên sản phẩm phải có đúng 12 ký tự")
                }

                try {
                    val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                    if (rate.scale() > 2) {
                        errorMessages.add("Tỉ lệ chỉ được tối đa 2 chữ số thập phân")
                    }
                } catch (e: NumberFormatException) {
                    errorMessages.add("Lỗi định dạng số trong cột tỉ lệ")
                }
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProduct = CompletionRateProduct(
                            productName = name,
                            rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                        )

                        completionRateProductRepository.add(compleRateProduct)
                    } else {
                        productExist.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))

                        completionRateProductRepository.update(productExist)
                    }
                    errorMessages.add("OK")
                    count++
                } catch (e: Exception) {
                    errorMessages.add("Có lỗi xảy ra khi cập nhật dữ liệu sản phẩm")
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
            fileName = "Ket_qua_import_san_pham.xlsx",
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
        val products = completionRateProcessRepository.findByKeywordPaginated(search, pageable)

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
                dataRow.createCell(0).setCellValue(item.key)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.rate.toString())
                dataRow.getCell(1).cellStyle = style


            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_ti_le_dat_cong_doan.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun importCsvProcess(file: MultipartFile, effectiveDate: LocalDateTime, expirationDate: LocalDateTime?): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productKeys = data.mapNotNull { x -> x[0] }

        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productKeys)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.key == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProcess(
                        key = item[0],
                        rate =  BigDecimal(item[1]),
                        processCode = item[0].toString().take(6),
                        layerCode = item[0].toString().substring(6, 7),
                        expirationDate = expirationDate,
                        effectiveDate = effectiveDate
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO

                        }
                        layers.add(layer)
                    }
                    completionRateProcessRepository.add(compleRateProduct)
                } else {
                    productExist.key = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProcessRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
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

                    )
            }, result.second
        )
    }

    fun importExcelCompletionRateProcess(file: MultipartFile, effectiveDate: LocalDateTime, expirationDate: LocalDateTime?) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productNames)
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == "Kết quả"
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue("Kết quả")
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            headerRow.getCell(headerCell).cellStyle.fillForegroundColor = IndexedColors.RED.index
            headerRow.getCell(headerCell).cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            sheet.setColumnWidth(headerCell, 15000)
        }

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val key = ExcelHelper.getCellValue(row, 0)
            val errorMessages = mutableListOf<String>()

            val productExist = productExists.find { x -> x.key == key }

            if (productExist == null) {
                if (key.length != 7) {
                    errorMessages.add("Key phải có đúng 7 ký tự")
                }

                try {
                    val rate = BigDecimal(ExcelHelper.getCellValue(row, 1))
                    if (rate.scale() > 2) {
                        errorMessages.add("Tỉ lệ chỉ được tối đa 2 chữ số thập phân")
                    }
                } catch (e: NumberFormatException) {
                    errorMessages.add("Lỗi định dạng số trong cột tỉ lệ")
                }
            }

            if (errorMessages.isEmpty()) {
                try {
                    if (productExist == null) {
                        val compleRateProduct = CompletionRateProcess(
                            key = key,
                            rate =  BigDecimal(ExcelHelper.getCellValue(row, 1)),
                            processCode = key.toString().take(6),
                            layerCode = key.toString().substring(6, 7),
                            expirationDate = expirationDate,
                            effectiveDate = effectiveDate
                        )

                        completionRateProcessRepository.add(compleRateProduct)
                    } else {
                        productExist.rate = BigDecimal(ExcelHelper.getCellValue(row, 1))

                        completionRateProcessRepository.update(productExist)
                    }
                    errorMessages.add("OK")
                    count++
                } catch (e: Exception) {
                    errorMessages.add("Có lỗi xảy ra khi cập nhật dữ liệu công đoạn")
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
            fileName = "Ket_qua_import_san_pham.xlsx",
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
                    rate = item.rate

                )
            }, result.second
        )
    }

    fun importCsvProcessProduct(file: MultipartFile, effectiveDate: LocalDateTime, expirationDate: LocalDateTime?): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productKeys = data.mapNotNull { x -> x[0] }

        val productExists = completionRateProcessProductRepository.getListProductByKey(productKeys)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.key == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProcessProduct(
                        key = item[0],
                        rate =  BigDecimal(item[1]),
                        productNameShortcut = item[0].toString().take(7),
                        processCode = item[0].substring(7, 13),
                        layerCode = item[0].substring(13, 14),
                        expirationDate = expirationDate,
                        effectiveDate = effectiveDate
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO

                        }
                        layers.add(layer)
                    }
                    completionRateProcessProductRepository.add(compleRateProduct)
                } else {
                    productExist.key = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProcessProductRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }




    fun exportCompletionRateProcessProductExcel(search: String?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val processproducts = completionRateProcessProductRepository.findByKeywordPaginated(search, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportCompleteRate.xlsx")
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

                dataRow.createCell(1).setCellValue(item.rate.toString())
                dataRow.getCell(1).cellStyle = style


            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_ti_le_dat_san_pham_cong_doan.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}