package com.kcvn.spm.app.productprocess.service

import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ExportExcelErrResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductProcessRepository
import org.apache.poi.ss.usermodel.CellType
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ProductProcessService(
    private val productProcessRep: ProductProcessRepository,
    private val processProcedureRep: ProcessProcedureStructureRepository,
    private val masterDataService: MasterDataService
) {
    fun getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): BasePagingResponse<ProductProcessResponse?> {
        val result = productProcessRep.findByKeywordPaginated(search, hasProcessConvertCode, pageable)
        val response = BasePagingResponse<ProductProcessResponse?>()
        response.data = result.first.map { productProcess ->
            ProductProcessResponse(
                processId = productProcess?.processId,
                processName = productProcess?.processName,
                processNameJp = productProcess?.processNameJp,
                processConvertCode = productProcess?.processConvertCode,
                processStatisticCode = productProcess?.processStatisticCode,
                processInventoryCode = productProcess?.processInventoryCode,
                productName = productProcess?.productName,
                layerCode = productProcess?.layerCode,
                processCode = productProcess?.processCode,
                productId = productProcess?.productId,
                processProcedureStructureId = productProcess?.processProcedureStructureId,
                layerCodeInt = productProcess?.layerCode!!.toInt(),
                processSequence = productProcess.processSequence
            )
        }
        response.totalRecords = result.second ?: 0

        return response
    }

    fun getProductProcessDetail(nameProduct: String?): List<ProductProcessResponse?>? {
        return productProcessRep.getByProductProcessDetail(nameProduct)
    }

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest): List<ProductProcess?> {
        val dataResult: MutableList<ProductProcess?> = mutableListOf()

        for (item in request.listProcess!!) {
            if (item.processId != null) {
                val productProcess = productProcessRep.getByProductProcessDetailById(item.processId)
                    ?: throw BusinessException(CommonUtils.getMessage("productProcess.notFound"))

                productProcess.processInventoryCode = item.processInventoryCode
                productProcess.processConvertCode = item.processConvertCode
                productProcess.processStatisticCode = item.processStatisticCode
                val data = productProcessRep.updateProcessDetail(productProcess)
                dataResult.add(data)
            } else {
                val requestProcessProcedureStructure = ImportProcessRequest()
                requestProcessProcedureStructure.productName = item.productName
                requestProcessProcedureStructure.layerCode = item.layerCode
                requestProcessProcedureStructure.processCode = item.processCode

                val queryProcessProcedureStructure = processProcedureRep.getByFilterProcessStructure(requestProcessProcedureStructure)

                val requestAddProcess = ProductProcess()
                requestAddProcess.processProcedureStructureId = queryProcessProcedureStructure?.id
                requestAddProcess.processConvertCode = item.processConvertCode
                requestAddProcess.processStatisticCode = item.processStatisticCode
                /// check điều kiện mã tồn kho khi khác null
//                if (item.processInventoryCode != null) {
//                    val productProcessAfter = request.listProcess!!.find { it.idx == item.idx + 1 }
//                    val productProcessPrev = request.listProcess!!.find { it.idx == item.idx - 1 }
//                    if ((productProcessAfter?.processCode!!.isNotEmpty() && productProcessAfter.processCode == item.processInventoryCode && productProcessAfter.layerCode == item.layerCode)
//                        || (productProcessPrev?.processCode!!.isNotEmpty() && productProcessPrev.processCode == item.processInventoryCode && productProcessPrev.layerCode == item.layerCode)
//                    ) {
//                        requestAddProcess.processInventoryCode = item.processInventoryCode;
//                    } else {
//                        throw BusinessException(CommonUtils.getMessage("processCode.notMap.processInventoryCode"))
//                    }
//                }
                requestAddProcess.processInventoryCode = item.processInventoryCode
                val data = productProcessRep.addProductProcess(requestAddProcess)
                dataResult.add(data)
            }
        }
        return dataResult
    }

    fun exportExcel(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): BaseResponse<FileContentModel> {
        val products = productProcessRep.findByKeywordPaginated(search, hasProcessConvertCode, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductProcessTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 2
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, item?.productName)
                ExcelHelper.setCellValue(dataRow, 1, style, item?.layerCode)
                ExcelHelper.setCellValue(dataRow, 2, style, item?.processCode)
                ExcelHelper.setCellValue(dataRow, 3, style, item?.processName)
                ExcelHelper.setCellValue(dataRow, 4, style, item?.processNameJp)
                ExcelHelper.setCellValue(dataRow, 5, style, item?.processConvertCode)
                ExcelHelper.setCellValue(dataRow, 6, style, item?.processInventoryCode)
                ExcelHelper.setCellValue(dataRow, 7, style, item?.processStatisticCode)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.process", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importProductProcessTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelProduct(file: MultipartFile): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
            throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val headerRow = sheet.getRow(0)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessTemplate.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        var count = 0
        val total = sheet.lastRowNum

        val masterData = masterDataService.getMasterDataSelection()

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        val dataErr: MutableList<ExportExcelErrResponse> = mutableListOf()
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val messageErr = ExportExcelErrResponse()
            val style = row.getCell(1).cellStyle

            messageErr.messageErrs = mutableListOf()
            var check = true
            if (ExcelHelper.getCellValue(row, 0).isEmpty()) {
                check = false
//                messageResults.add(
//                    CommonUtils.getMessage(
//                        "validate.excel.empty",
//                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
//                    )
//                )
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                ))
            }
            if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
                check = false
//                messageResults.add(
//                    CommonUtils.getMessage(
//                        "validate.excel.empty",
//                        arrayOf(ExcelHelper.getCellValue(headerRow, 1))
//                    )
//                )
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 1))
                ))
            }
            if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
                check = false
//                messageResults.add(
//                    CommonUtils.getMessage(
//                        "validate.excel.empty",
//                        arrayOf(ExcelHelper.getCellValue(headerRow, 1))
//                    )
//                )

                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                ))
            }
            if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
                check = false
//                messageResults.add(
//                    CommonUtils.getMessage(
//                        "validate.excel.empty",
//                        arrayOf(ExcelHelper.getCellValue(headerRow, 2))
//                    )
//                )

                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                ))
            }

            if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
                check = false
//                messageResults.add(
//                    CommonUtils.getMessage(
//                        "validate.excel.empty",
//                        arrayOf(ExcelHelper.getCellValue(headerRow, 4))
//                    )
//                )

                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 5))
                ))
            }

            if (ExcelHelper.getCellValue(row, 0).isNotEmpty() && ExcelHelper.getCellValue(row, 0).length > 12) {
                check = false
//                messageResults.add(CommonUtils.getMessage(
//                    "validate.excel.maxLength",
//                    arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)))

                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)))
            }
            if (ExcelHelper.getCellValue(row, 1).isNotEmpty() && ExcelHelper.getCellValue(row, 1).length > 8) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength",arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)))
            }
            if (ExcelHelper.getCellValue(row, 2).isNotEmpty() && ExcelHelper.getCellValue(row, 2).length > 4) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength",arrayOf(ExcelHelper.getCellValue(headerRow, 2), 4)))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 2), 4)))
            }
            if (ExcelHelper.getCellValue(row, 3).isNotEmpty() && ExcelHelper.getCellValue(row, 3).length > 10) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength",arrayOf(ExcelHelper.getCellValue(headerRow, 3), 6)))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 3), 6)))
            }
            if (ExcelHelper.getCellValue(row, 4).isNotEmpty() && ExcelHelper.getCellValue(row, 4).length > 10) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength",arrayOf(ExcelHelper.getCellValue(headerRow, 4), 10)))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 4), 10)))
            }
            if (ExcelHelper.getCellValue(row, 5).isNotEmpty() && ExcelHelper.getCellValue(row, 5).length > 10) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength",arrayOf(ExcelHelper.getCellValue(headerRow, 5), 10)))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 5), 10)))
            }

            if (!masterData.processConvertCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 3) }) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.notExist",arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.notExist",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
            }
            if (!masterData.processStatisticCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 5) }) {
                check = false
//                messageResults.add(CommonUtils.getMessage("validate.excel.notExist",arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.notExist",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
            }

            messageErr.productName = ExcelHelper.getCellValue(row, 0)
            messageErr.processCode = if (row.getCell(1).cellType == CellType.NUMERIC && row.getCell(1).numericCellValue % 1 == 0.0)
                row.getCell(1).numericCellValue.toInt().toString()
            else {
                ExcelHelper.getCellValue(row, 1)
            }
            messageErr.layerCode = if (row.getCell(2).cellType == CellType.NUMERIC && row.getCell(2).numericCellValue % 1 == 0.0)
                row.getCell(2).numericCellValue.toInt().toString()
            else {
                ExcelHelper.getCellValue(row, 1)
            }
            messageErr.processConvertCode = ExcelHelper.getCellValue(row, 3)
            messageErr.processInventoryCode = ExcelHelper.getCellValue(row, 4)
            messageErr.processStatisticCode = ExcelHelper.getCellValue(row, 5)

            try {
                if (check) {
                    val cellProcessCode = row.getCell(1)
                    var processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                        cellProcessCode.numericCellValue.toInt().toString()
                    else {
                        ExcelHelper.getCellValue(row, 1)
                    }

                    val cellLayerCode = row.getCell(2)
                    var layerCode = ""
                    if (cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0)
                        layerCode = cellLayerCode.numericCellValue.toInt().toString()
                    else {
                        processCode = ExcelHelper.getCellValue(row, 2)
                    }
                    val filter = ImportProcessRequest(
                        productName = ExcelHelper.getCellValue(row, 0),
                        processCode = processCode,
                        layerCode = layerCode
                    )
                    val filterCheckProcessProcedure = processProcedureRep.getByFilterProcessStructure(filter)
                    if (filterCheckProcessProcedure == null) {
//                       messageResults.add(CommonUtils.getMessage("validate.excel.process.dataNull"))
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.process.dataNull",
                        ))
                    } else {
                        val requestImport = ProductProcess(
                            processProcedureStructureId = filterCheckProcessProcedure.id,
                            processConvertCode = ExcelHelper.getCellValue(row, 3),
                            processInventoryCode = ExcelHelper.getCellValue(row, 4),
                            processStatisticCode = ExcelHelper.getCellValue(row, 5),
                        )
                        val checkProductProcess = productProcessRep.findByIdProductProcedureStructure(filterCheckProcessProcedure.id)
                        if (checkProductProcess == null) {
                            requestImport.createdDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            requestImport.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                            productProcessRep.insertProductProcess(requestImport)
                        } else {
                            requestImport.updatedBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                            requestImport.updatedDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            productProcessRep.updateProcessDetail(requestImport)
                        }
                        // messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))

                        count++
                    }
                }
            } catch (e: Exception) {
                //messageResults.add(CommonUtils.getMessage("validate.excel.process.data.update.err"))
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.process.data.update.err",
                ))
            }

            dataErr.add(messageErr)
            // val result = messageResults.joinToString(separator = "; ")

//            if (row.getCell(colIndexResult) == null) {
//                row.createCell(colIndexResult)
//            }
            // row.getCell(colIndexResult ).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = style
        }

//        val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
//        for (row in resultRows) {
//            val rowNum = row.rowNum
//            sheet.removeRow(row)
//            if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
//                sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
//            }
//        }
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        workbook.write(byteArrayOutputStream)
//
//        val excelBytes = byteArrayOutputStream.toByteArray()

        val excelBytes = exportExcelErr(dataErr)

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.result.import", arrayOf(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )
        workbook.close()

        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }

    fun exportExcelErr(requestErr: MutableList<ExportExcelErrResponse>): ByteArray? {
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessTemplate.xlsx")

        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)

        ExcelHelper.createColResult(headerRow, sheet)

        if (requestErr.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in requestErr) {
                if (item.messageErrs.isNullOrEmpty()) continue
                else {
                    val dataRow: Row = sheet.createRow(rowNumber++)
                    ExcelHelper.setCellValue(dataRow, 0, style, item.productName)
                    ExcelHelper.setCellValue(dataRow, 1, style, item.processCode)
                    ExcelHelper.setCellValue(dataRow, 2, style, item.layerCode)
                    ExcelHelper.setCellValue(dataRow, 3, style, item.processConvertCode)
                    ExcelHelper.setCellValue(dataRow, 4, style, item.processInventoryCode)
                    ExcelHelper.setCellValue(dataRow, 5, style, item.processStatisticCode)
                    ExcelHelper.setCellValue(dataRow, 6, style, item.messageErrs?.joinToString(separator = "; "))
                }
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }
}