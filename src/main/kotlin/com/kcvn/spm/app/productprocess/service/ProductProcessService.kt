package com.kcvn.spm.app.productprocess.service

import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.request.ItemUpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ExportExcelErrResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.MasterDataType
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.CommonCategoryRepository
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
    private val masterDataService: MasterDataService,
    private val commonCategoryRep: CommonCategoryRepository
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
        val masterData = masterDataService.getMasterDataSelection()
        val query =  productProcessRep.getByProductProcessDetail(nameProduct)
        val data = query?.map {
            val listProcessCode = commonCategoryRep.getListProcessCodeDropDown(it?.processCode, MasterDataType.MACHUYENDOI)
            val listStatisticCode = commonCategoryRep.getListProcessCodeDropDown(it?.processCode,MasterDataType.MATHONGKE)
            val productProcessResponse = ProductProcessResponse()
            if(listProcessCode.isNullOrEmpty()){
                productProcessResponse.listDropDownConvertCode = masterData.processConvertCodes
            }else
                productProcessResponse.listDropDownConvertCode= listProcessCode

            if (listStatisticCode.isNullOrEmpty()){
                productProcessResponse.listDropDownStatisticCode = masterData.processStatisticCodes
            }else
                productProcessResponse.listDropDownStatisticCode= listStatisticCode


            productProcessResponse.processId = it?.processId
            productProcessResponse.productName = it?.productName
            productProcessResponse.layerCode = it?.layerCode
            productProcessResponse.processCode = it?.processCode
            productProcessResponse.processName = it?.processName
            productProcessResponse.processNameJp = it?.processNameJp
            productProcessResponse.processConvertCode = it?.processConvertCode
            productProcessResponse.processStatisticCode = it?.processStatisticCode
            productProcessResponse.processInventoryCode = it?.processInventoryCode
            productProcessResponse.idx = it?.idx
            productProcessResponse.productId = it?.productId
            productProcessResponse.processProcedureStructureId = it?.processProcedureStructureId
            productProcessResponse.layerCodeInt = it?.layerCodeInt
            productProcessResponse.processSequence = it?.processSequence
            productProcessResponse.dayOfImplementation = it?.dayOfImplementation
            productProcessResponse.inventoryLayerGroup = it?.inventoryLayerGroup
            productProcessResponse
        }
        data?.forEachIndexed { idx, dt ->
            dt.idx = idx
        }
        return data
    }

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest): List<ProductProcess?> {
        val dataResult: MutableList<ProductProcess?> = mutableListOf()
        //// Lấy ra danh sách công đoạn cuối mỗi lớp và check phải tồn tại mã tồn kho là mã công đoạn ở lớp trước
        val listInventoryProcessGrByLayer = request.listProcess
            ?.filter { it.layerCode?.toInt() != 1 }
            ?.groupBy { it.layerCode }
        for(itemInventoryProcessGrByLayer in listInventoryProcessGrByLayer!!.values){
            val itemInventoryProcessGrByLayerSort = itemInventoryProcessGrByLayer.sortedBy { it.idx }
            val itemInventoryProcessGrByLayerLast = itemInventoryProcessGrByLayerSort.last()
            if(itemInventoryProcessGrByLayerLast.processInventoryCode.isNullOrEmpty()
                || itemInventoryProcessGrByLayerLast.inventoryLayerGroup.isNullOrEmpty()){
                throw BusinessException(CommonUtils.getMessage("validate.excel.checkInventoryLastNull"))
            }else {
                val layerCodeItem =  itemInventoryProcessGrByLayerLast.layerCode?.toIntOrNull() ?: 0
                val layerCodePre = layerCodeItem - 1
                request.listProcess?.firstOrNull{
                    it.processCode == itemInventoryProcessGrByLayerLast.processInventoryCode
                            && it.layerCode?.toInt() == layerCodePre
                } ?: throw BusinessException(CommonUtils.getMessage("validate.excel.checkInventoryLast"))
            }
        }


        ///// Check khi ngày thứ thực hiện để trống và check ngày thứ thực hiện phải có ngày bắt đầu từ 1
        val countDayOfImplementNotNull = request.listProcess?.count { it.dayOfImplementation != null } ?: 0
        val countRequest = request.listProcess?.count()
        if(countDayOfImplementNotNull > 0 && countDayOfImplementNotNull != countRequest){
            throw BusinessException(CommonUtils.getMessage("validate.excel.dayOfImplementation"))
        }else if(countDayOfImplementNotNull > 0)  {
            request.listProcess?.firstOrNull{
                it.dayOfImplementation == 1
            } ?: throw  BusinessException(CommonUtils.getMessage("validate.excel.processDayOne"))
        }
        // check ngày thứ thực hiện nếu k trống thì trong lớp phải có ngày thực hiện bắt đầu bằng 1

        /// check mã tồn kho hoặc mã thống kê để null
        val listConvertCodeOrStatisticCodeNull = request.listProcess?.firstOrNull{
            it.processConvertCode.isEmpty() || it.processStatisticCode.isEmpty()
        }
        if(listConvertCodeOrStatisticCodeNull != null){
            throw  BusinessException(CommonUtils.getMessage("validate.convertCode.statisticCode.null"))
        }
        // check nếu tồn tại Lớp số gộp tồn kho thì phải tồn tại mã gộp tồn kho
        val checkInventoryCodeAndLayerCodeGr = request.listProcess?.firstOrNull{
            (!it.inventoryLayerGroup.isNullOrEmpty() && it.processInventoryCode.isNullOrEmpty())
                    || (it.inventoryLayerGroup.isNullOrEmpty() && !it.processInventoryCode.isNullOrEmpty())
        }
        if(checkInventoryCodeAndLayerCodeGr != null){
            throw  BusinessException(CommonUtils.getMessage("validate.convertCode.inventoryCodeAndInventoryLayerGr1"))
        }
        // Tạo ra model để lưu lại dữ liệu trước đó trong for
        var requestPre: ItemUpdateProductProcessDetailRequest? = null
        /// add hoặc update dữ liệu
        val dataAdd: MutableList<ProductProcess> = mutableListOf()
        val dataUpdate: MutableList<ProductProcess> = mutableListOf()
        for (item in request.listProcess!!) {
            if(item.isEdit == true){
                // Check ngày thứ thực hiện nếu khác null
                if(requestPre?.layerCode == item.layerCode){
                    val dayOfImplementation = item.dayOfImplementation ?: 0
                    val dayOfImplementationPreInt = requestPre!!.dayOfImplementation ?: 0
                    if(dayOfImplementation - dayOfImplementationPreInt > 1
                        || dayOfImplementation - dayOfImplementationPreInt < 0){
                        throw  BusinessException(CommonUtils.getMessage("validate.convertCode.checkSubtractionDayOfImplementation"))
                    }
                }

                if (item.processId != null) {
                    val productProcess = productProcessRep.getByProductProcessDetailById(item.processId)
                        ?: throw BusinessException(CommonUtils.getMessage("productProcess.notFound"))
                    productProcess.processInventoryCode = item.processInventoryCode
                    productProcess.processConvertCode = item.processConvertCode
                    productProcess.processStatisticCode = item.processStatisticCode
                    productProcess.inventoryLayerGroup = item.inventoryLayerGroup
                    productProcess.dayOfImplementation = item.dayOfImplementation
                    dataUpdate.add(productProcess)
                    //val data = productProcessRep.updateProcessDetail(productProcess)
                    //dataResult.add(data)
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
                    requestAddProcess.inventoryLayerGroup = item.inventoryLayerGroup
                    requestAddProcess.dayOfImplementation = item.dayOfImplementation
                    requestAddProcess.processInventoryCode = item.processInventoryCode
                    dataAdd.add(requestAddProcess)
                    //val data = productProcessRep.addProductProcess(requestAddProcess)
                    //dataResult.add(data)
                }
                requestPre = item
            }
            else {
                requestPre = item
                continue
            }
        }
        if(dataAdd.isNotEmpty()){
            for (item in dataAdd){
                val data = productProcessRep.addProductProcess(item)
                dataResult.add(data)
            }
        }
        if(dataUpdate.isNotEmpty()){
            for (item in dataUpdate){
                val data = productProcessRep.updateProcessDetail(item)
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
                ExcelHelper.setCellValue(workbook, dataRow, 0, style, item?.productName)
                ExcelHelper.setCellValue(workbook, dataRow, 1, style, item?.layerCode)
                ExcelHelper.setCellValue(workbook, dataRow, 2, style, item?.processCode)
                ExcelHelper.setCellValue(workbook, dataRow, 3, style, item?.processName)
                ExcelHelper.setCellValue(workbook, dataRow, 4, style, item?.processNameJp)
                ExcelHelper.setCellValue(workbook, dataRow, 5, style, item?.processConvertCode)
                ExcelHelper.setCellValue(workbook, dataRow, 6, style, item?.processInventoryCode)
                ExcelHelper.setCellValue(workbook, dataRow, 7, style, item?.processStatisticCode)
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
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                ))
            }
            if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 1))
                ))
            }
            if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                ))
            }
            if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                ))
            }

            if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.empty",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 5))
                ))
            }

            if (ExcelHelper.getCellValue(row, 0).isNotEmpty() && ExcelHelper.getCellValue(row, 0).length > 12) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)))
            }
            if (ExcelHelper.getCellValue(row, 1).isNotEmpty() && ExcelHelper.getCellValue(row, 1).length > 8) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)))
            }
            if (ExcelHelper.getCellValue(row, 2).isNotEmpty() && ExcelHelper.getCellValue(row, 2).length > 4) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 2), 4)))
            }
            if (ExcelHelper.getCellValue(row, 3).isNotEmpty() && ExcelHelper.getCellValue(row, 3).length > 10) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 3), 6)))
            }
            if (ExcelHelper.getCellValue(row, 4).isNotEmpty() && ExcelHelper.getCellValue(row, 4).length > 10) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 4), 10)))
            }
            if (ExcelHelper.getCellValue(row, 5).isNotEmpty() && ExcelHelper.getCellValue(row, 5).length > 10) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 5), 10)))
            }

            if (!masterData.processConvertCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 3) }) {
                check = false
                messageErr.messageErrs?.add(CommonUtils.getMessage(
                    "validate.excel.notExist",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
            }
            if (!masterData.processStatisticCodes.any { x -> x.label == ExcelHelper.getCellValue(row, 5) }) {
                check = false
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
                    val processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                        cellProcessCode.numericCellValue.toInt().toString()
                    else {
                        ExcelHelper.getCellValue(row, 1)
                    }

                    val cellLayerCode = row.getCell(2)
                    val layerCode = if (cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0) {
                         cellLayerCode.numericCellValue.toInt().toString()
                    } else {
                         ExcelHelper.getCellValue(row, 2)
                    }
                    val filter = ImportProcessRequest(
                        productName = ExcelHelper.getCellValue(row, 0),
                        processCode = processCode,
                        layerCode = layerCode
                    )
                    val filterCheckProcessProcedure = processProcedureRep.getByFilterProcessStructure(filter)
                    if (filterCheckProcessProcedure == null) {
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

    fun importExcelProduct1(file: MultipartFile): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
            throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val headerRow = sheet.getRow(0)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessTemplate.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val total = sheet.lastRowNum

        val masterData = masterDataService.getMasterDataSelection()
        //val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
        // Tạo lít dataErr để sau trả lại excel lỗi
        val dataErr: MutableList<ExportExcelErrResponse> = mutableListOf()
        val dataDefault: MutableList<ProductProcessResponse> = mutableListOf()
        // đọc file lấy dữ liệu ở file excel lưu vào 1 list default
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val process = ProductProcessResponse()
            val cellProcessCode = row.getCell(1)
            val processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                cellProcessCode.numericCellValue.toInt().toString()
            else {
                ExcelHelper.getCellValue(row, 1)
            }

            val cellLayerCode = row.getCell(2)
            val layerCode = if (cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0) {
                cellLayerCode.numericCellValue.toInt().toString()
            } else {
                ExcelHelper.getCellValue(row, 2)
            }
            process.productName = ExcelHelper.getCellValue(row, 0)
            process.processCode = processCode
            process.layerCode = layerCode
            process.processConvertCode = ExcelHelper.getCellValue(row, 3)
            val processInventoryCode =  if(ExcelHelper.getCellValue(row, 4).isEmpty()){
                ""
            }else {
                if (row.getCell(4).cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0) {
                    row.getCell(4).numericCellValue.toInt().toString()
                } else {
                    ExcelHelper.getCellValue(row, 4)
                }
            }
            process.processInventoryCode = processInventoryCode
            process.processStatisticCode = ExcelHelper.getCellValue(row, 5)
            val inventoryLayerGroup = if(ExcelHelper.getCellValue(row, 6).isEmpty()){
                ""
            }else {
                if (row.getCell(6).cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0) {
                    row.getCell(6).numericCellValue.toInt().toString()
                } else {
                    ExcelHelper.getCellValue(row, 6)
                }
            }

            val dayOfImplementation = if(ExcelHelper.getCellValue(row, 7).isEmpty()){
                ""
            }else {
                if (row.getCell(7).cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0) {
                    row.getCell(7).numericCellValue.toInt().toString()
                } else {
                    ExcelHelper.getCellValue(row, 7)
                }
            }
            process.inventoryLayerGroup = inventoryLayerGroup
            process.dayOfImplementation = dayOfImplementation

            dataDefault.add(process)
        }

        // list product name lỗi
        val listKeyErr : MutableList<String?> = mutableListOf()
        // gr theo key product name
        val dataDefaultGr = dataDefault.groupBy { it.productName }
        // Lấy ra list product name default
        var listProductNameDefault: List<String?> = listOf()
        for(itemProduct in dataDefaultGr){
            listProductNameDefault = listProductNameDefault + itemProduct.key
        }
        val listProductNameDefaultNotNull = listProductNameDefault.filterNotNull()
        // lấy ra dữ liệu theo tên sản phẩm dựa vào list product name trong excel
        val listDataDb = processProcedureRep.getByProductName(listProductNameDefaultNotNull)
        // validate dữ liệu và thêm vào bản ghi

        for (listItem in dataDefaultGr){

            var checkList = true
            var checkCountProcess = true
            var checkDayNull = true
            var checkDayOne = true
            // Đếm xem có đủ công đoạn trong db không
            val countProcess = listDataDb.count { it.productCode == listItem.key }
            val countProcessByExcel = listItem.value.count { it.processCode?.any { char -> char != '0' } ?: false }
            if(countProcess != countProcessByExcel){

                checkList = false
                checkCountProcess = false
            }

            val countDayImplement = listItem.value.count{ !it.dayOfImplementation.isNullOrEmpty()}
            val countListItem = listItem.value.count()
            if(countDayImplement != countListItem && countDayImplement > 0){
                checkDayNull = false
            }
            if(countDayImplement == countListItem && countDayImplement > 0){
                val findDayOne = listItem.value.firstOrNull{
                    it.dayOfImplementation!!.toDoubleOrNull() == 1.0}
                if(findDayOne == null){
                    checkDayOne = false
                }
            }

            if(!checkCountProcess || !checkDayNull || !checkDayOne ){
                for(item in listItem.value){
                    val messageErr = ExportExcelErrResponse()
                    messageErr.messageErrs = mutableListOf()
                    messageErr.productName = item.productName
                    messageErr.processCode = item.processCode
                    messageErr.layerCode = item.layerCode
                    messageErr.processConvertCode = item.processConvertCode
                    messageErr.processInventoryCode = item.processInventoryCode
                    messageErr.processStatisticCode = item.processStatisticCode
                    messageErr.inventoryLayerGroup = item.inventoryLayerGroup
                    messageErr.dayOfImplementation = item.dayOfImplementation
                    if(!checkCountProcess){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.sumProcessByProduct"))
                    }
                    if(!checkDayNull){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.dayOfImplementation"))
                    }
                    if(!checkDayOne){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.processDayOne"))
                    }

                    dataErr.add(messageErr)
                }
            }

            var i = 0
            val listItemValueMap = listItem.value.sortedWith(compareBy({ it.layerCode }, { it.processSequence })).map { x ->
                i++
                val sq = listDataDb.firstOrNull {
                    it.productCode == x.productName
                            && it.processCode == x.processCode
                            && it.layerCode == x.layerCode}
                val model = ProductProcessResponse(
                    processSequence = sq?.processSequence.toString(),
                    processName = x.processName,
                    productName = x.productName,
                    processCode = x.processCode,
                    layerCode = x.layerCode,
                    processConvertCode = x.processConvertCode,
                    processStatisticCode = x.processStatisticCode,
                    processInventoryCode = x.processInventoryCode,
                    inventoryLayerGroup = x.inventoryLayerGroup,
                    dayOfImplementation = x.dayOfImplementation,
                    idx = i
                )
                model
            }
            // Check mã tồn kho từ lớp 2 trở đi ở cuối phải là mã công đoạn ở lớp trước đó
            val listProcessLast: MutableList<ProductProcessResponse> = mutableListOf()
            val listItemGrByLayer = listItemValueMap.filter { it.layerCode != "1" }
                .groupBy { it.layerCode }
            for(item in listItemGrByLayer){
                val listItemGrByLayerSort = item.value.sortedBy { it.processSequence }
                val itemLast = listItemGrByLayerSort.last()
                listProcessLast.add(itemLast)
            }

            val listItemValueMapSort = listItemValueMap.sortedWith(compareBy({ it.layerCode }, { it.processSequence }))
            var itemInventoryLayerGrPre: ProductProcessResponse? =  null
            var k = 0;
            for (item in listItemValueMapSort ){
                val messageErr = ExportExcelErrResponse()
                messageErr.messageErrs = mutableListOf()
                val checkDataErr = dataErr.firstOrNull { it.processCode == item.processCode
                        && it.productName == item.productName
                        && it.layerCode == item.layerCode}
                if (checkDataErr != null) {
                    continue
                }
                if (item.productName.isNullOrEmpty()) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                    ))
                }
                if (item.processCode.isNullOrEmpty()) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1))
                    ))
                }
                if (item.layerCode.isNullOrEmpty()) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                    ))
                }
                if (item.processConvertCode.isNullOrEmpty()) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                    ))
                }

                if (item.processStatisticCode.isNullOrEmpty()) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5))
                    ))
                }

                if (!item.productName.isNullOrEmpty() && item.productName!!.length > 12) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)))
                }
                if (!item.processCode.isNullOrEmpty() && item.processCode!!.length > 6) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)))
                }
                if (!item.layerCode.isNullOrEmpty() && item.layerCode!!.length > 2) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2), 4)))
                }
                if (!item.processConvertCode.isNullOrEmpty() && item.processConvertCode!!.length > 10) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3), 6)))
                }
                if (!item.processInventoryCode.isNullOrEmpty() && item.processInventoryCode!!.length > 10) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4), 10)))
                }
                if (!item.processStatisticCode.isNullOrEmpty() && item.processStatisticCode!!.length > 10) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5), 10)))
                }

                if (!masterData.processConvertCodes.any { x -> x.label == item.processConvertCode }) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.notExist",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
                }
                if (!masterData.processStatisticCodes.any { x -> x.label == item.processStatisticCode }) {
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.notExist",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
                }



                if((!item.processInventoryCode.isNullOrEmpty() && item.inventoryLayerGroup.isNullOrEmpty())
                    || (item.processInventoryCode.isNullOrEmpty() && !item.inventoryLayerGroup.isNullOrEmpty()))
                {
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.inventoryCodeAndInventoryLayerGr1"))
                    checkList = false
                }

                if(!item.inventoryLayerGroup.isNullOrEmpty()){
                    val checkInventoryLayerGr = listItem.value.firstOrNull { it.layerCode == item.inventoryLayerGroup
                            && it.processCode == item.processInventoryCode}
                    if(checkInventoryLayerGr == null){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.inventoryLayerGr"))
                        checkList = false
                    }
                }

                val checkProcessStructure = listDataDb.firstOrNull {
                    it.productCode == item.productName
                            && it.layerCode?.toInt() == item.layerCode?.toInt()
                            && it.processCode == item.processCode
                }
                if(checkProcessStructure == null){
                    checkList = false
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.process.dataNull"))
                    checkList = false
                }else {
                    messageErr.idProcessStructure = checkProcessStructure.id
                }

                if(itemInventoryLayerGrPre != null && !item.dayOfImplementation.isNullOrEmpty() && itemInventoryLayerGrPre.layerCode == item.layerCode) {
                    val dayItemInventoryLayerGrPre = itemInventoryLayerGrPre.dayOfImplementation?.toIntOrNull() ?: 0
                    val dayItemInventoryLayerGr = item.dayOfImplementation?.toIntOrNull() ?: 0
                    if(dayItemInventoryLayerGr - dayItemInventoryLayerGrPre > 1 || dayItemInventoryLayerGr - dayItemInventoryLayerGrPre < 0){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.checkSubtractionDayOfImplementation"))
                        checkList = false
                    }
                }

                if(!item.processInventoryCode.isNullOrEmpty() && item.inventoryLayerGroup.isNullOrEmpty()){
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.inventoryCodeAndInventoryLayerGr1"))
                    checkList = false
                }

                if(item.processInventoryCode.isNullOrEmpty() && !item.inventoryLayerGroup.isNullOrEmpty()){
                    messageErr.messageErrs?.add(CommonUtils.getMessage(
                        "validate.excel.inventoryCodeAndInventoryLayerGr2"))
                    checkList = false
                }

                if(!item.processInventoryCode.isNullOrEmpty() && !item.inventoryLayerGroup.isNullOrEmpty()){
                    if(item.processInventoryCode == item.processCode && item.inventoryLayerGroup == item.layerCode){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.inventoryCodeAndInventoryLayerGr3"))
                        checkList = false
                    }else {
                        val check = listItem.value.firstOrNull { it.processCode == item.processInventoryCode
                                && it.layerCode == item.inventoryLayerGroup}
                        if(check == null){
                            messageErr.messageErrs?.add(CommonUtils.getMessage(
                                "validate.excel.inventoryCodeAndInventoryLayerGr4"))
                            checkList = false
                        }
                        val idx = item.idx ?: 0
                        val check1 = listItem.value.firstOrNull {
                                it.processInventoryCode == item.processCode
                                && it.inventoryLayerGroup == item.layerCode
                                && k <= idx
                        }
                        if(check1 != null && !check1.processInventoryCode.isNullOrEmpty()){
                            messageErr.messageErrs?.add(CommonUtils.getMessage(
                                "validate.excel.inventoryCodeAndInventoryLayerGr5",
                                arrayOf(item.processCode.toString(), item.layerCode.toString()
                                , check1.processCode.toString(), check1.layerCode.toString()))
                            )
                            checkList = false
                        }
                    }

                }
                k++

                val checkLastProcess = listProcessLast.firstOrNull{
                    it.productName == item.productName
                            && it.layerCode == item.layerCode
                            && it.processCode == item.processCode
                }
                if(checkLastProcess != null){
                    val layerItemInt = item.layerCode?.toIntOrNull() ?: 0
                    val listProcessLayerPre = listItem.value.filter {
                        it.layerCode?.toIntOrNull() == (layerItemInt - 1)
                                && it.productName == item.productName
                    }
                    val checkProcessInventoryLast = listProcessLayerPre.firstOrNull{
                        it.processCode == checkLastProcess.processInventoryCode
                    }
                    if(checkProcessInventoryLast == null){
                        messageErr.messageErrs?.add(CommonUtils.getMessage(
                            "validate.excel.checkInventoryLast"))
                        checkList = false
                    }
                }

                itemInventoryLayerGrPre = item

                messageErr.productName = item.productName
                messageErr.layerCode = item.layerCode
                messageErr.processCode = item.processCode
                messageErr.processConvertCode = item.processConvertCode
                messageErr.processInventoryCode = item.processInventoryCode
                messageErr.processStatisticCode = item.processStatisticCode
                messageErr.inventoryLayerGroup = item.inventoryLayerGroup
                messageErr.dayOfImplementation = item.dayOfImplementation
                dataErr.add(messageErr)
            }
            if(!checkList) {
                listKeyErr.add(listItem.key)
            }
        }
        val resultDataErr: MutableList<ExportExcelErrResponse> = mutableListOf()
        for (itemKey in listKeyErr){
            val filteredData = dataErr.filter { it.productName == itemKey }
            resultDataErr.addAll(filteredData)
        }
        var count = 0
        val resultData = dataErr.filter { it !in resultDataErr }.toMutableList()
        for(item in resultData){
            val requestImport = ProductProcess(
                processProcedureStructureId = item.idProcessStructure,
                processConvertCode = item.processConvertCode,
                processInventoryCode = item.processInventoryCode,
                processStatisticCode = item.processStatisticCode,
                inventoryLayerGroup =  item.inventoryLayerGroup,
                dayOfImplementation = item.dayOfImplementation?.toIntOrNull()
            )
            val checkProductProcess = productProcessRep.findByIdProductProcedureStructure(requestImport.processProcedureStructureId)
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
        val excelBytes = exportExcelErr(resultDataErr)

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

                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(workbook,dataRow, 0, style, item.productName)
                ExcelHelper.setCellValue(workbook,dataRow, 1, style, item.processCode)
                ExcelHelper.setCellValue(workbook,dataRow, 2, style, item.layerCode)
                ExcelHelper.setCellValue(workbook,dataRow, 3, style, item.processConvertCode)
                ExcelHelper.setCellValue(workbook,dataRow, 4, style, item.processInventoryCode)
                ExcelHelper.setCellValue(workbook,dataRow, 5, style, item.processStatisticCode)
                ExcelHelper.setCellValue(workbook,dataRow, 6, style, item.inventoryLayerGroup)
                ExcelHelper.setCellValue(workbook,dataRow, 7, style, item.dayOfImplementation)

                val resultCellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
                ExcelHelper.setCellValue(workbook, dataRow, 8, resultCellStyle, item.messageErrs?.joinToString(separator = "; "))

            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }


}