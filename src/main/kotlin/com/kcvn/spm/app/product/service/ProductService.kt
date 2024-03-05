package com.kcvn.spm.app.product.service

import com.fasterxml.jackson.core.type.TypeReference
import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.product.payload.model.LayerImportProductModel
import com.kcvn.spm.app.product.payload.model.ProcessGroupModel
import com.kcvn.spm.app.product.payload.model.ProductModel
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductDetailResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.JsonConvert
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val masterDataService: MasterDataService,
    private val productProcessRep: ProductProcessRepository,
    private val processGroupRep: ProcessGroupRepository
) {

    fun getListProduct(request: ProductSearchRequest?, pageable: Pageable): PagingProductResponse {
        val products = productRep.getPagingList(request, pageable)
        var response = PagingProductResponse()

        if (products.first.isNotEmpty()) {
            response = mappingProductResponse(products.first)
            response.totalRecords = products.second
        }

        return response
    }

    fun getProductDetail(request: String?): ProductDetailResponse? {
        return productRep.getProductDetail(request)
    }

    fun exportExcel(request: ProductSearchRequest?, pageable: Pageable): BaseResponse<FileContentModel> {
        val products = productRep.getList(request, pageable)
        val productMapping = mappingProductResponse(products)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (!productMapping.data.isNullOrEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var headerCol = 18
            val layers = productMapping.data!!.asSequence().mapNotNull { x ->
                if (x.productLayerDetail.isNullOrEmpty()) null
                else {
                    val type = object : TypeReference<List<LayerImportProductModel>>() {}
                    JsonConvert.deserialize<List<LayerImportProductModel>>(x.productLayerDetail!!, type)
                }
            }.flatten().mapNotNull { x -> x.layerCode?.toIntOrNull() }.distinct().sortedBy { x -> x }.toList()

            val headerRow: Row = sheet.getRow(1)
            val headerStyle = headerRow.getCell(11).cellStyle
            if (layers.isNotEmpty()) {
                for (col in layers) {
                    ExcelHelper.setCellValue(headerRow, headerCol, headerStyle, CommonUtils.getMessage("excel.colLayerName", arrayOf(col)))
                    headerCol++
                }
            }
            if (!productMapping.columns.isNullOrEmpty()) {
                for (col in productMapping.columns!!) {
                    ExcelHelper.setCellValue(headerRow, headerCol, headerStyle, col.value)
                    headerCol++
                }
            }

            var rowNumber = 2
            for (item in productMapping.data!!) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, item.name)
                ExcelHelper.setCellValue(dataRow, 1, style, item.name?.substring((if (item.name!!.length < 7) 0 else item.name!!.length - 7), item.name!!.length))
                ExcelHelper.setCellValue(dataRow, 2, style, item.exportType)
                ExcelHelper.setCellValue(dataRow, 3, style, item.size)
                ExcelHelper.setCellValue(dataRow, 4, style, item.frame_1)
                ExcelHelper.setCellValue(dataRow, 5, style, item.frame_2)
                ExcelHelper.setCellValue(dataRow, 6, style, item.mold)
                ExcelHelper.setCellValue(dataRow, 7, style, item.productLine)
                ExcelHelper.setCellValue(dataRow, 8, style, item.srNosr)
                ExcelHelper.setCellValue(dataRow, 9, style, item.pcsSh?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 10, style, item.shBlock?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 11, style, item.layerCount?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 12, style, item.ringJig)
                ExcelHelper.setCellValue(dataRow, 13, style, item.process?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 14, style, item.completionRate?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 15, style, item.snapMold)
                ExcelHelper.setCellValue(dataRow, 16, style, item.tapeCommon)
                ExcelHelper.setCellValue(dataRow, 17, style, item.tapeType)

                var colIndex = 18
                if (!item.productLayerDetail.isNullOrEmpty()) {
                    val type = object : TypeReference<List<LayerImportProductModel>>() {}
                    val layerValues = JsonConvert.deserialize<List<LayerImportProductModel>>(item.productLayerDetail!!, type).sortedBy { x -> x.layerCode }
                    for (layer in layers) {
                        val cellValue = layerValues.find { x -> x.layerCode?.toIntOrNull() == layer }
                        ExcelHelper.setCellValue(dataRow, colIndex, style, cellValue?.value?.toString())
                        colIndex++
                    }
                }

                if (!productMapping.columns.isNullOrEmpty()) {
                    for (col in productMapping.columns!!) {
                        val cellValue = item.lstProcess.find { x -> x.key == col.key }
                        ExcelHelper.setCellValue(dataRow, colIndex, style, cellValue?.value)
                        colIndex++
                    }
                }
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportListProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importProductTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelProduct(file: MultipartFile): BaseResponse<FileContentModel> {
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)

        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))

            val headerRow = sheet.getRow(0)

            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 16))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

            val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
            val productExists = productRep.getByName(productNames)
            val masterData = masterDataService.getMasterDataSelection()
            var count = 0
            var total = 0

            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
            val productNameInserts = mutableListOf<String>()
            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val style = row.getCell(0).cellStyle
                val name = ExcelHelper.getCellValue(row, 0)
                val messageResults = validateImportProduct(row, headerRow, masterData)
                if (productNameInserts.any { x -> x == name }) messageResults.add(CommonUtils.getMessage("validate.excel.duplicate"))
                total++
                if (messageResults.isEmpty()) {
                    val productExist = productExists.find { x -> x.name == name }
                    try {
                        if (productExist == null) {
                            val product = Product(
                                name = ExcelHelper.getCellValue(row, 0),
                                exportType = ExcelHelper.getCellValue(row, 1),
                                size = ExcelHelper.getCellValue(row, 2),
                                frame_1 = ExcelHelper.getCellValue(row, 3),
                                frame_2 = ExcelHelper.getCellValue(row, 4),
                                mold = ExcelHelper.getCellValue(row, 5),
                                productLine = ExcelHelper.getCellValue(row, 6),
                                srNosr = ExcelHelper.getCellValue(row, 7),
                                pcsSh = ExcelHelper.getCellValue(row, 8).toBigDecimalOrNull()?.toInt(),
                                shBlock = ExcelHelper.getCellValue(row, 9).toBigDecimalOrNull()?.toInt(),
                                layerCount = ExcelHelper.getCellValue(row, 10).toBigDecimalOrNull()?.toInt(),
                                ringJig = ExcelHelper.getCellValue(row, 11),
                                process = ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull()?.toInt(),
                                snapMold = ExcelHelper.getCellValue(row, 13),
                                tapeCommon = ExcelHelper.getCellValue(row, 14),
                                tapeType = ExcelHelper.getCellValue(row, 15),
                            )

                            val layers = mutableListOf<LayerImportProductModel>()
                            for (i in 16 until colIndexResult) {
                                if (ExcelHelper.getCellValue(row, i).isEmpty()) continue
                                val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toBigDecimalOrNull()?.toInt())
                                layers.add(layer)
                            }

                            product.productLayerDetail = JsonConvert.serialize(layers)

                            productRep.add(product)
                        } else {
                            productExist.exportType = ExcelHelper.getCellValue(row, 1)
                            productExist.size = ExcelHelper.getCellValue(row, 2)
                            productExist.frame_1 = ExcelHelper.getCellValue(row, 3)
                            productExist.frame_2 = ExcelHelper.getCellValue(row, 4)
                            productExist.mold = ExcelHelper.getCellValue(row, 5)
                            productExist.productLine = ExcelHelper.getCellValue(row, 6)
                            productExist.srNosr = ExcelHelper.getCellValue(row, 7)
                            productExist.pcsSh = ExcelHelper.getCellValue(row, 8).toBigDecimalOrNull()?.toInt()
                            productExist.shBlock = ExcelHelper.getCellValue(row, 9).toBigDecimalOrNull()?.toInt()
                            productExist.layerCount = ExcelHelper.getCellValue(row, 10).toBigDecimalOrNull()?.toInt()
                            productExist.ringJig = ExcelHelper.getCellValue(row, 11)
                            productExist.process = ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull()?.toInt()
                            productExist.snapMold = ExcelHelper.getCellValue(row, 13)
                            productExist.tapeCommon = ExcelHelper.getCellValue(row, 14)
                            productExist.tapeType = ExcelHelper.getCellValue(row, 15)

                            val layers = mutableListOf<LayerImportProductModel>()
                            for (i in 16 until colIndexResult) {
                                if (ExcelHelper.getCellValue(row, i).isEmpty()) continue
                                val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toBigDecimalOrNull()?.toInt())
                                layers.add(layer)
                            }

                            productExist.productLayerDetail = JsonConvert.serialize(layers)

                            productRep.update(productExist)
                        }
                        messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                        count++
                        productNameInserts.add(name)
                    } catch (e: Exception) {
                        messageResults.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                    }
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

            val resultRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
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
                fileName = CommonUtils.getMessage("fileName.resultImportProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
                content = excelBytes
            )

            workbook.close()
            return BaseResponse(
                response,
                if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
            )
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    private fun mappingProductResponse(products: List<Product>): PagingProductResponse {
        val productNames = products.mapNotNull { x -> x.name }
        val completionRates = completionRateProductRep.getEffectiveByProduct(productNames)
        val productProcedureStructures = processProcedureStructureRep.getByProductName(productNames)
        val procedureStructureIds = productProcedureStructures.mapNotNull { x -> x.id }
        val productProcesses = productProcessRep.getByProcessProcedureStructure(procedureStructureIds)
        val processGroups = processGroupRep.getAll()
        val productProcessGroups = productProcesses.filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
        }.map { x ->
            val procedureStructure = productProcedureStructures.find { m -> m.id == x.processProcedureStructureId }
            if (procedureStructure == null) ProcessGroupModel()
            else ProcessGroupModel(procedureStructure.productCode, x.processStatisticCode)
        }.filter { x -> !x.processStatisticCode.isNullOrEmpty() && !x.productName.isNullOrEmpty() }
            .groupBy { x -> Pair(x.productName, x.processStatisticCode) }

        val response = PagingProductResponse()
        response.data = products.map { x ->
            val prod = ProductModel(
                id = x.id,
                name = x.name,
                exportType = x.exportType,
                size = x.size,
                frame_1 = x.frame_1,
                frame_2 = x.frame_2,
                mold = x.mold,
                productLine = x.productLine,
                srNosr = x.srNosr,
                pcsSh = x.pcsSh,
                shBlock = x.shBlock,
                layerCount = x.layerCount,
                ringJig = x.ringJig,
                snapMold = x.snapMold,
                tapeCommon = x.tapeCommon,
                tapeType = x.tapeType,
                completionRate = completionRates?.find { m -> m.productName == x.name }?.rate?.toDouble(),
                productLayerDetail = x.productLayerDetail
            )
            val lstProcess = productProcessGroups.filter { m -> m.key.first == x.name }.mapNotNull { m -> KeyValueResponse(m.key.second, m.value.size.toString()) }.toMutableList()
            if (lstProcess.any { m -> m.key == ProcessStatisticCode.HP_TAN || m.key == ProcessStatisticCode.HP_ALL }) {
                val sum = (lstProcess.find { m -> m.key == ProcessStatisticCode.HP_TAN }?.value?.toInt() ?: 0) +
                    (lstProcess.find { m -> m.key == ProcessStatisticCode.HP_ALL }?.value?.toInt() ?: 0)
                lstProcess.add(KeyValueResponse(ProcessStatisticCode.IN_LO, sum.toString()))
            }
            if (lstProcess.any { m -> m.key == ProcessStatisticCode.TAN || m.key == ProcessStatisticCode.ZEN }) {
                val sum = (lstProcess.find { m -> m.key == ProcessStatisticCode.TAN }?.value?.toInt() ?: 0) +
                    (lstProcess.find { m -> m.key == ProcessStatisticCode.ZEN }?.value?.toInt() ?: 0)
                lstProcess.add(KeyValueResponse(ProcessStatisticCode.IN_MACH, sum.toString()))
            }
            if (lstProcess.any { m -> m.key == ProcessStatisticCode.M_TAN || m.key == ProcessStatisticCode.M_ALL }) {
                val sum = (lstProcess.find { m -> m.key == ProcessStatisticCode.M_TAN }?.value?.toInt() ?: 0) +
                    (lstProcess.find { m -> m.key == ProcessStatisticCode.M_ALL }?.value?.toInt() ?: 0)
                lstProcess.add(KeyValueResponse(ProcessStatisticCode.GHEP_LOP, sum.toString()))
            }
            prod.lstProcess = lstProcess.toList()
            prod
        }

        val columns = productProcesses.asSequence().filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
        }.map { x ->
            val processGroup = processGroups.find { m -> !m.processStatisticCode.isNullOrEmpty() && m.processStatisticCode == x.processStatisticCode }
            if (processGroup == null) KeyValueResponse()
            else KeyValueResponse(x.processStatisticCode, processGroup.description, processGroup.sortOrder)
        }.filter { x -> !x.key.isNullOrEmpty() && !x.value.isNullOrEmpty() }.distinct().toMutableList()

        if (columns.any { m -> m.key == ProcessStatisticCode.HP_TAN || m.key == ProcessStatisticCode.HP_ALL }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.IN_LO }
            if (processGroup != null) columns.add(KeyValueResponse(processGroup.processStatisticCode, processGroup.description, processGroup.sortOrder))
        }
        if (columns.any { m -> m.key == ProcessStatisticCode.TAN || m.key == ProcessStatisticCode.ZEN }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.IN_MACH }
            if (processGroup != null) columns.add(KeyValueResponse(processGroup.processStatisticCode, processGroup.description, processGroup.sortOrder))
        }
        if (columns.any { m -> m.key == ProcessStatisticCode.M_TAN || m.key == ProcessStatisticCode.M_ALL }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.GHEP_LOP }
            if (processGroup != null) columns.add(KeyValueResponse(processGroup.processStatisticCode, processGroup.description, processGroup.sortOrder))
        }
        response.columns = columns.sortedBy { x -> x.sort }.distinct().toList()
        return response
    }

    private fun validateImportProduct(row: Row, headerRow: Row, masterData: MasterDataSelectionResponse): MutableList<String> {
        val messageResults = mutableListOf<String>()
        if (ExcelHelper.getCellValue(row, 0).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
        } else {
            if (ExcelHelper.getCellValue(row, 0).length != 12)
                messageResults.add(CommonUtils.getMessage("validate.excel.length", arrayOf(ExcelHelper.getCellValue(headerRow, 0), "12")))
        }
        if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
        } else {
            if (!masterData.exportTypeSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 1) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
        }
        if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
        }
        if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        } else {
            if (!masterData.frame1Selections.any { x -> x.label == ExcelHelper.getCellValue(row, 3) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        }
        if (ExcelHelper.getCellValue(row, 4).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        } else {
            if (!masterData.frame2Selections.any { x -> x.label == ExcelHelper.getCellValue(row, 4) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        }
        if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        } else {
            if (!masterData.moldSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 5) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        }
        if (ExcelHelper.getCellValue(row, 7).isNotEmpty() && !masterData.srNosrSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 7) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 7))))
        }
        if (ExcelHelper.getCellValue(row, 8).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 8))))
        }
        if (ExcelHelper.getCellValue(row, 9).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 9))))
        }
        if (ExcelHelper.getCellValue(row, 10).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 10))))
        }
        if (ExcelHelper.getCellValue(row, 11).isNotEmpty() && !masterData.ringJigSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 11) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 11))))
        }
        if (ExcelHelper.getCellValue(row, 12).isNotEmpty() && ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull() == null) {
            messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 12))))
        }
        if (ExcelHelper.getCellValue(row, 14).isNotEmpty() && !masterData.tapeCommonSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 14) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 14))))
        }
        if (ExcelHelper.getCellValue(row, 15).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 15))))
        } else {
            if (!masterData.tapeTypeSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 15) }) {
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 15))))
            }
        }
        val frame1 = ExcelHelper.getCellValue(row, 3)
        val mold = ExcelHelper.getCellValue(row, 5)
        if (frame1.isNotEmpty() && mold.isNotEmpty()) {
            when (frame1) {
                Constants.KHUNG1_ML -> {
                    if (mold != Constants.KHUONDUC_ML)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }

                Constants.KHUNG1_MU -> {
                    if (mold != Constants.KHUONDUC_KVC && mold != Constants.KHUONDUC_SKE)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }

                Constants.KHUNG1_SWR -> {
                    if (mold != Constants.KHUONDUC_SWR && mold != Constants.KHUONDUC_SUR)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }
            }
        }
        return messageResults
    }
}