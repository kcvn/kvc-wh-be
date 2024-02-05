package com.kcvn.spm.app.product.service

import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.product.payload.model.LayerImportProductModel
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.helper.jsonhelper.JsonConvert
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val masterDataService: MasterDataService
) {

    fun getListProduct(request: ProductSearchRequest?, pageable: Pageable) : PagingProductResponse {
        val products = productRep.getPagingList(request, pageable)
        var response = PagingProductResponse()

        if (products.first.isNotEmpty()) {
            response = mappingProductResponse(products.first)
            response.totalRecords = products.second
        }

        return response
    }

    fun getProductDetail(request: String): Product? {
        return productRep.getProductDetail(request)
    }

    fun exportExcel(request: ProductSearchRequest?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = productRep.getList(request, pageable)
        val productMapping = mappingProductResponse(products)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (!productMapping.data.isNullOrEmpty()) {
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

            if (!productMapping.columns.isNullOrEmpty()) {
                var headerCol = 12
                val headerRow: Row = sheet.getRow(1)
                val headerStyle = headerRow.getCell(11).cellStyle
                for (col in productMapping.columns!!) {
                    headerRow.createCell(headerCol).setCellValue(col.value)
                    headerRow.getCell(headerCol).cellStyle = headerStyle
                    headerCol++
                }
            }

            var rowNumber = 2
            for (item in productMapping.data!!) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.name)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.exportType)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.size)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.frame_1)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.frame_2)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item.mold)
                dataRow.getCell(5).cellStyle = style

                dataRow.createCell(6).setCellValue(item.productLine)
                dataRow.getCell(6).cellStyle = style

                dataRow.createCell(7).setCellValue(item.srNosr)
                dataRow.getCell(7).cellStyle = style

                dataRow.createCell(8).setCellValue(item.pcsSh?.toString() ?: "")
                dataRow.getCell(8).cellStyle = style

                dataRow.createCell(9).setCellValue(item.shBlock?.toString() ?: "")
                dataRow.getCell(9).cellStyle = style

                dataRow.createCell(10).setCellValue(item.layerCount?.toString() ?: "")
                dataRow.getCell(10).cellStyle = style

                dataRow.createCell(11).setCellValue(item.completionRate?.toString() ?: "")
                dataRow.getCell(11).cellStyle = style

                if (!productMapping.columns.isNullOrEmpty()) {
                    var cellIndex = 12
                    for (col in productMapping.columns!!) {
                        val cellValue = item.lstProcess.find { x -> x.key == col.key }
                        dataRow.createCell(cellIndex).setCellValue(cellValue?.value ?: "")
                        dataRow.getCell(cellIndex).cellStyle = style
                        cellIndex++
                    }
                }
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_san_pham.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "ImportProductTemplate.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelProduct(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = productRep.getByName(productNames)
        val masterData = masterDataService.getMasterDataSelection()
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val headerCell = sheet.first().lastCellNum + 1
        val headerRow = sheet.getRow(0)
        val headerStyle = headerRow.getCell(0).cellStyle
        headerRow.createCell(headerCell).setCellValue("Kết quả")
        headerRow.getCell(headerCell).cellStyle = headerStyle


        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            //val style = row.getCell(0).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            val messageResults = mutableListOf<String>()
            var check = true
            if (!masterData.exportTypeSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 1) }) {
                check = false
                messageResults.add("Loại xuất hàng không tồn tại")
            }
            if (!masterData.frame1Selections.any { x -> x.value == ExcelHelper.getCellValue(row, 3) }) {
                check = false
                messageResults.add("Khung 1 không tồn tại")
            }
            if (!masterData.frame2Selections.any { x -> x.value == ExcelHelper.getCellValue(row, 4) }) {
                check = false
                messageResults.add("Khung 2 không tồn tại")
            }
            if (!masterData.moldSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 5) }) {
                check = false
                messageResults.add("Khuôn đục không tồn tại")
            }
            if (!masterData.srNosrSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 7) }) {
                check = false
                messageResults.add("S.R/No S.R không tồn tại")
            }
            if (!masterData.ringJigSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 11) }) {
                check = false
                messageResults.add("RING/JIG không tồn tại")
            }
            if (!masterData.tapeCommonSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 14) }) {
                check = false
                messageResults.add("Tape dùng chung không tồn tại")
            }
            if (!masterData.tapeTypeSelections.any { x -> x.value == ExcelHelper.getCellValue(row, 15) }) {
                check = false
                messageResults.add("Loại tape không tồn tại")
            }

            if (check) {
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
                            pcsSh = ExcelHelper.getCellValue(row, 8).toInt(),
                            shBlock = ExcelHelper.getCellValue(row, 9).toInt(),
                            layerCount = ExcelHelper.getCellValue(row, 10).toInt(),
                            ringJig = ExcelHelper.getCellValue(row, 11),
                            process = ExcelHelper.getCellValue(row, 12).toInt(),
                            snapMold = ExcelHelper.getCellValue(row, 13),
                            tapeCommon = ExcelHelper.getCellValue(row, 14),
                            tapeType = ExcelHelper.getCellValue(row, 15),
                        )

                        val layers = mutableListOf<LayerImportProductModel>()
                        for (i in 16 until row.lastCellNum) {
                            if (ExcelHelper.getCellValue(row, i).isEmpty()) continue
                            val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toInt())
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
                        productExist.pcsSh = ExcelHelper.getCellValue(row, 8).toInt()
                        productExist.shBlock = ExcelHelper.getCellValue(row, 9).toInt()
                        productExist.layerCount = ExcelHelper.getCellValue(row, 10).toInt()
                        productExist.ringJig = ExcelHelper.getCellValue(row, 11)
                        productExist.process = ExcelHelper.getCellValue(row, 12).toInt()
                        productExist.snapMold = ExcelHelper.getCellValue(row, 13)
                        productExist.tapeCommon = ExcelHelper.getCellValue(row, 14)
                        productExist.tapeType = ExcelHelper.getCellValue(row, 15)

                        val layers = mutableListOf<LayerImportProductModel>()
                        for (i in 16 until row.lastCellNum) {
                            if (row.getCell(i).stringCellValue.isNullOrEmpty()) continue
                            val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toInt())
                            layers.add(layer)
                        }

                        productExist.productLayerDetail = JsonConvert.serialize(layers)

                        productRep.update(productExist)
                    }
                    messageResults.add("OK")
                    count++
                } catch (e: Exception) {
                    messageResults.add("Có lỗi xảy ra khi cập nhật dữ liệu sản phẩm")
                }
            }
            val result = messageResults.joinToString(separator = "\n")
            row.createCell(row.lastCellNum + 0).setCellValue(result)
            //row.getCell(row.lastCellNum + 1).cellStyle = style
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
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }



    private fun mappingProductResponse(products: List<Product>) : PagingProductResponse {
        val productNames = products.mapNotNull { x -> x.name }
        val completionRates = completionRateProductRep.getByProduct(productNames)
        val productProcesses = processProcedureStructureRep.getByProductName(productNames)
        val processGroups = productProcesses.groupBy { x -> Pair(x.productCode, x.processCode) }

        val response = PagingProductResponse()
        response.data = products.map { x -> ProductResponse(
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
                completionRate = (completionRates.find { m -> m.productName == x.name }?.rate ?: 0.0).toDouble(),
                lstProcess = processGroups.filter { m -> m.key.first == x.name }.mapNotNull { m -> DropdownResponse(m.key.second, m.value.size.toString()) }
        ) }

        response.columns = productProcesses.map { x -> DropdownResponse(x.processCode,x.processCode) }.distinct()

        return response
    }
}