package com.kcvn.spm.sample.service

import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductProcessRepository
import org.apache.poi.ss.usermodel.*
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

@Service
@Transactional
class ProductProcessService(
    private val productProcessRep : ProductProcessRepository,
    private val processProcedureRep : ProcessProcedureStructureRepository,
    private val masterDataService : MasterDataService
) {
    fun  getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): BasePagingResponse<ProductProcessResponse>
    {
        val result = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode,pageable);
        val response = BasePagingResponse<ProductProcessResponse>();
            response.data = result.first.map { productProcess ->
                ProductProcessResponse(
                    processId = productProcess.processId,
                    processName = productProcess.processName,
                    processNameJp = productProcess.processNameJp,
                    processConvertCode = productProcess.processConvertCode,
                    processStatisticCode = productProcess.processStatisticCode,
                    processInventoryCode = productProcess.processInventoryCode,
                    productName = productProcess.productName,
                    layerCode = productProcess.layerCode,
                    processCode = productProcess.processCode,
                    productId = productProcess.productId,
                );
            }
            response.totalRecords = result.second ?: 0

        return response;
    }

    fun getProductProcessDetail(nameProduct: String?) : List<ProductProcessResponse?>?{
        return productProcessRep.getByProductProcessDetail(nameProduct)
    }

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest) : List<ProductProcess?> {
        val dataResult: MutableList<ProductProcess?> = mutableListOf()
        for (item in request.listProcess!!){
            val productProcess = productProcessRep.getByProductProcessDetailById(item.processId)
                ?: throw BusinessException(CommonUtils.getMessage("productProcess.notFound"))
            if (item.processInventoryCode != null){
               val productProcessAfter =  request.listProcess!!.find {  it.idx == item.idx + 1 }
                if((productProcessAfter?.processCode != null &&  productProcessAfter.processCode != item.processInventoryCode) )
                {
                    throw BusinessException(CommonUtils.getMessage("processCode.notMap.processInventoryCode"))
                }
            }
            productProcess.processConvertCode = item.processConvertCode;
            productProcess.processStatisticCode = item.processStatisticCode;
            productProcess.processInventoryCode = item.processInventoryCode;

            val data = productProcessRep.updateProcessDetail(productProcess);
            dataResult.add(data)
        }
        return  dataResult
    }

    fun exportExcel(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductProcessTemplate.xlsx")
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


            var rowNumber = 2
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.productName)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.layerCode)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.processCode)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.processName)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.processNameJp)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item.processConvertCode)
                dataRow.getCell(5).cellStyle = style

                dataRow.createCell(6).setCellValue(item.processInventoryCode)
                dataRow.getCell(6).cellStyle = style

                dataRow.createCell(7).setCellValue(item.processStatisticCode)
                dataRow.getCell(7).cellStyle = style
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.process"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "ImportProcessTemplate.xlsx",
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
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        val masterData = masterDataService.getMasterDataSelection()

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
            val messageResults = mutableListOf<String>()
            var check = true
            if(row.getCell(0) == null){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
            }
            if(row.getCell(1) == null){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
            }
            if(row.getCell((2)) == null){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
            }
            if(row.getCell(3) == null){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
            }
            if(row.getCell(5) == null){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
            }

            if(row.getCell(0) != null && row.getCell(0).toString().length > 60){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.maxLength"))
            }
            if(row.getCell(1) !=null && row.getCell(1).toString().length > 8){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.process.code.length"))
            }
            if(row.getCell((2)) != null && row.getCell(2).toString().length >4){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.process.layer.code.length"))
            }
            if(row.getCell(3) != null && row.getCell(3).toString().length > 10){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.process.convert.code.length"))
            }
            if(row.getCell(4) != null && row.getCell(4).toString().length > 10){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.process.inventory.code.length"))
            }
            if(row.getCell(5) != null && row.getCell(5).toString().length > 10){
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.process.statistic.code.length"))
            }

            if (!masterData.processConvertCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 3) }) {
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist",arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
            }
            if (!masterData.processStatisticCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 5) }) {
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist",arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
            }

           try {
               if (check) {
                   val cellProcessCode = row.getCell(1)
                   var processCode = ""
                   if(cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                       processCode = cellProcessCode.numericCellValue.toInt().toString()
                   else {
                       processCode = ExcelHelper.getCellValue(row, 1)
                   }

                   val cellLayerCode = row.getCell(2)
                   var layerCode = ""
                   if(cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0)
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
                   if(filterCheckProcessProcedure == null)
                   {
                       messageResults.add(CommonUtils.getMessage("validate.excel.process.data.null"))
                   }
                   else {
                       val requestImport = ProductProcess(
                           processProcedureStructureId = filterCheckProcessProcedure.id,
                           processConvertCode = ExcelHelper.getCellValue(row, 3),
                           processInventoryCode = ExcelHelper.getCellValue(row, 4),
                           processStatisticCode = ExcelHelper.getCellValue(row, 5),
                       )
                       val checkProductProcess = productProcessRep.findByIdProductProcedureStructure(filterCheckProcessProcedure.id)
                       if(checkProductProcess == null){
                           requestImport.createdDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                           requestImport.createdBy = CommonUtils.loggedInUser() ?: "SYSTEM"
                           productProcessRep.insertProductProcess(requestImport)
                       }
                       else {
                           requestImport.updatedBy = CommonUtils.loggedInUser() ?: "SYSTEM"
                           requestImport.updatedDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                           productProcessRep.updateProcessDetail(requestImport)
                       }
                       messageResults.add("OK")
                       count++
                   }
               }
           }
           catch (e: Exception){
               messageResults.add(CommonUtils.getMessage("validate.excel.process.data.update.err"))
           }
            val result = messageResults.joinToString(separator = "; ")

            if (!checkColResult) {
                row.createCell(row.lastCellNum + 0).setCellValue(result)
                row.getCell(row.lastCellNum - 1).cellStyle = style
            }
            else{
                row.getCell(row.lastCellNum - 1).setCellValue(result)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.result.import"),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )
        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }
}