package com.kcvn.spm.app.backlogwh.service

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchForSendingRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhUpdateRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.app.backlogwh.payload.response.BacklogHistoryResponse
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhForSendingResponse
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.BacklogWhHistory
import com.kcvn.spm.model.tables.pojos.NewBacklogWh
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.plus

@Service
@Transactional
class BacklogWhService(
    private val backlogWhRepo: BacklogWhRepository,
    private val backlogWhHistoryRepo: BacklogWhHistoryRepository,
    private val receivingTransRepo: ReceivingTransactionsRepository,
    private val movingTransRepo: MovingRepository,
    private val sendingTransRepo: SendingTransactionsRepository

) {
    fun plusBacklog(locationCode: String, data: NewBacklogWh, transactionType: String, refId: String, plusBoxQty: Boolean) {
        val existing = backlogWhRepo.findByPackageCodeAndLotNo(data.packageCode!!, data.lotNo!!)
        if (existing == null) {
            backlogWhRepo.save(data)
            saveHistory(locationCode, data.poNumber, data.packageCode, data.lotNo, data.backlogQty, data.boxQty, data.receivingDate, transactionType, refId)
        } else {
            existing.backlogQty = existing.backlogQty!!.plus(data.backlogQty!!)
            if (plusBoxQty)
                existing.boxQty = existing.boxQty!!.plus(data.boxQty!!)
            backlogWhRepo.update(existing)
            saveHistory(locationCode, existing.poNumber, existing.packageCode, existing.lotNo, existing.backlogQty, existing.boxQty, existing.receivingDate, transactionType, refId)
        }
    }

        fun minusBacklog(locationCode: String, data: NewBacklogWh, transactionType: String, refId: String, minusBoxQty: Boolean) {
            val existing = backlogWhRepo.findByPackageCodeAndLotNo(data.packageCode!!, data.lotNo!!)
            if (existing == null || existing.backlogQty!! < data.backlogQty!!) {
                throw BusinessException("Không đủ tồn kho ${data.packageCode} - ${data.lotNo}")
            } else {
                existing.backlogQty = existing.backlogQty!!.minus(data.backlogQty!!)
                existing.boxQty = existing.boxQty!!.minus(data.boxQty!!)
                backlogWhRepo.update(existing)
                saveHistory(locationCode, existing.poNumber, existing.packageCode, existing.lotNo, existing.backlogQty, existing.boxQty, existing.receivingDate, transactionType, refId)
            }
        }

    /** moving khong doi so luong ton, chi doi BIN => ghi history voi ton hien tai, locationCode = BIN dich */
    fun saveMovingHistory(destLocationCode: String?, backlog: BacklogWh, refId: String?) {
        saveHistory(destLocationCode, backlog.poNumber, backlog.packageCode, backlog.lotNo, backlog.backlogQty, backlog.boxQty, backlog.receivingDate, "TRANSFER", refId)
    }

    // backlogQty/boxQty: ton kho sau khi thay doi
    private fun saveHistory(
        locationCode: String?, poNumber: String?, packageCode: String?, lotNo: String?,
        backlogQty: BigDecimal?, boxQty: Int?, receivingDate: LocalDate?, transactionType: String, refId: String?,
    ) {
        backlogWhHistoryRepo.save(
            BacklogWhHistory(
                locationCode = locationCode,
                poNumber = poNumber,
                packageCode = packageCode,
                lotNo = lotNo,
                backlogQty = backlogQty,
                boxQty = boxQty,
                receivingDate = receivingDate,
                transactionType = transactionType,
            ),
            refId
        )
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    fun downloadTemplate(): BaseResponse<FileContentModel> {
//        val templateStream = this::class.java.classLoader.getResourceAsStream("assets/template/ImportBinEntryTemplate.xlsx")
//            ?: throw FileNotFoundException("ImportBinEntryTemplate.xlsx file not found in resources.")
//
//        val workbook = templateStream.use { XSSFWorkbook(it) }
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportBinEntryTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.use { it.write(byteArrayOutputStream) }

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ImportBinEntryTemplate.xlsx"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = byteArrayOutputStream.toByteArray()
        )

        return BaseResponse(response)
    }

    fun importBinEntry(file: MultipartFile): BaseResponse<List<ImportBacklogWh>> {
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportBinEntryTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1
            val headerRow = sheet.getRow(0)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 6))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val updatedList = mutableListOf<ImportBacklogWh>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val status = ExcelHelper.getCellValue(row, 5)
                if (status != "Success") continue

                val backlogWhData = ImportBacklogWh(
                    receivingDate = ExcelHelper.getCellValueDateAmoeba(row, 0),
                    poNumber = ExcelHelper.getCellValue(row, 1),
                    locationCode = ExcelHelper.getCellValue(row, 2),
                    inspectionDate = ExcelHelper.getCellValueDateAmoeba(row, 4),
                    isEntried = true,
                    itemName = ExcelHelper.getCellValue(row, 6)
                )

                backlogWhRepo.updateInspectionDate(backlogWhData)
                updatedList.add(backlogWhData)
            }
            return BaseResponse(updatedList, CommonUtils.getMessage("Updated"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun exportBacklogWhExcel(request: BacklogWhSearchRequest, pageable: Pageable?): BaseResponse<FileContentModel> {
        val listBacklogResponse = backlogWhRepo.getList(request, pageable)

//        val inputStream = this::class.java.classLoader.getResourceAsStream("assets/template/ExportBacklogWhTemplate.xlsx")
//            ?: throw FileNotFoundException("ExportBacklogWhTemplate.xlsx file not found in resources.")
//        val workbook = XSSFWorkbook(inputStream)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportBacklogWhTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val rowNumber = 0
        val dataRow: Row = sheet.getRow(rowNumber) ?: sheet.createRow(rowNumber)
        val style = ExcelHelper.getCellStyleCommon(workbook)
        style.alignment = HorizontalAlignment.CENTER

        val numberStyle = workbook.createCellStyle()
        numberStyle.cloneStyleFrom(style)
        numberStyle.alignment = HorizontalAlignment.RIGHT

        val numberFormat = workbook.createDataFormat().getFormat("#,##0")

        val listBacklog = listBacklogResponse.first

        var rowNumberFill = 1
        for (item in listBacklog) {
            val row: Row = sheet.createRow(rowNumberFill++)

            val formattedReceivingDate = item.receivingDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: ""
            ExcelHelper.setCellValue(row, 0, style, formattedReceivingDate)
            ExcelHelper.setCellValue(row, 1, style, item.packageCode)
            ExcelHelper.setCellValue(row, 2, style, item.poNumber)
            ExcelHelper.setCellValue(row, 3, style, item.itemName)
            ExcelHelper.setCellValue(row, 4, style, item.lotNo)
            ExcelHelper.setCellValue(row, 5, style, item.issueDate)
            ExcelHelper.setCellValue(row, 6, style, item.locationCode)
            ExcelHelper.setCellValueInt(row, 7, numberStyle, item.backlogQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValueInt(row, 8, numberStyle, item.boxQty ?: 0, numberFormat)
        }

        //sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ExportBacklog.xlsx", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun exportBinEntry(pageable: Pageable): BaseResponse<FileContentModel> {
        val listBacklogResponse = backlogWhRepo.getBinEntryList(pageable)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportBinEntryTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val rowNumber = 0
        val dataRow: Row = sheet.getRow(rowNumber) ?: sheet.createRow(rowNumber)
        val style = ExcelHelper.getCellStyleCommon(workbook)
        style.alignment = HorizontalAlignment.CENTER

        val numberStyle = workbook.createCellStyle()
        numberStyle.cloneStyleFrom(style)
        numberStyle.alignment = HorizontalAlignment.RIGHT

        val numberFormat = workbook.createDataFormat().getFormat("#,##0")

        val listBacklog = listBacklogResponse.first

        var rowNumberFill = 1
        for (item in listBacklog) {
            val row: Row = sheet.createRow(rowNumberFill++)

            val formattedReceivingDate = item.receivingDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) ?: ""
            val formattedInspectionDate = item.inspectionDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) ?: ""
            ExcelHelper.setCellValue(row, 0, style, formattedReceivingDate)
            ExcelHelper.setCellValue(row, 1, style, item.poNumber)
            ExcelHelper.setCellValue(row, 2, style, item.locationCode)
            ExcelHelper.setCellValueInt(row, 4, numberStyle, item.backlogQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValue(row, 5, style, formattedInspectionDate)
        }

        for (i in 1 until rowNumberFill) {
            val row = sheet.getRow(i) ?: sheet.createRow(i)
            val formulaCell = row.createCell(3, CellType.FORMULA)
            formulaCell.cellFormula = "TEXT(E${i + 1},\"#,##0.000\")"
            formulaCell.cellStyle = numberStyle
        }
        sheet.forceFormulaRecalculation = true

        sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ExportBinEntry.xlsx", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun oldPlusBacklog(data: BacklogWh, transactionType: String, refId: String?) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
        // get receiving date

        val receivingDate = data.receivingDate
        if (backlog == null) {
            val entityBacklog = BacklogWh(
                null,
                data.locationCode,
                data.poNumber,
                data.packageCode,
                data.backlogQty,
                data.boxQty,
                receivingDate,
                isEntried = data.isEntried,
                inspectionDate = data.inspectionDate,
                lotNo = data.lotNo,
                issueDate = data.issueDate
            )
            backlogWhRepo.save(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null,
                data.locationCode,
                data.poNumber,
                data.packageCode,
                data.backlogQty,
                data.boxQty,
                receivingDate,
                data.inspectionDate,
                transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory, refId)
        } else {
            val newLotNo = if (backlog.lotNo?.contains(data.lotNo?: "") == true) backlog.lotNo else backlog.lotNo + ";" + data.lotNo
            val newIssueDate = if (backlog.issueDate?.contains(data.issueDate?: "") == true) backlog.issueDate else backlog.issueDate + ";" + data.issueDate
            val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!),
                data.boxQty?.plus(backlog.boxQty!!), isEntried = data.isEntried, lotNo = newLotNo, issueDate = newIssueDate
            )
            //backlogWhRepo.update(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!), data.boxQty?.plus(backlog.boxQty!!), receivingDate, data.inspectionDate, transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory, refId)
        }
    }

    fun oldMinusBacklog(data: BacklogWh, transactionType: String, refId: String?) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
            ?: throw BusinessExceptionDetail(
                CommonUtils.getMessage("data.not.found.in.backlog ${data.locationCode} - ${data.packageCode} - ${data.poNumber}"), "locationCode = ${data.locationCode}, packageCode = ${data.packageCode}, poNumber = ${data.poNumber}"
            )
        if (backlog.backlogQty!! < data.backlogQty) throw BusinessExceptionDetail(
            CommonUtils.getMessage("not.enough.backlog ${data.locationCode} - ${data.packageCode} - ${data.poNumber}"), "locationCode = ${data.locationCode}, packageCode = ${data.packageCode}, poNumber = ${data.poNumber}")
        val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, backlog.backlogQty?.minus(data.backlogQty!!),
            backlog.boxQty?.minus(data.boxQty!!), data.receivingDate, data.inspectionDate, isEntried = data.isEntried, lotNo = backlog.lotNo, issueDate = backlog.issueDate
        )
        //backlogWhRepo.update(entityBacklog)
        // insert backlog history
        val entityBacklogHistory = BacklogWhHistory(
            null, entityBacklog.locationCode, entityBacklog.poNumber, entityBacklog.packageCode, entityBacklog.backlogQty, entityBacklog.boxQty, data.receivingDate, data.inspectionDate, transactionType
        )
        backlogWhHistoryRepo.save(entityBacklogHistory, refId)
    }

    fun update(request: List<BacklogWhUpdateRequest>) {
        request.forEach {
            val backlog = backlogWhRepo.findByPackageCode(it.packageCode!!)
                backlog.forEach {
                    backlogWhRepo.updateQty(it.packageCode!!, it.backlogQty!!, it.boxQty!!)
                    // insert backlog history
                    val entityBacklogHistory = BacklogWhHistory(
                        null, it.locationCode, it.poNumber, it.packageCode, it.backlogQty, it.boxQty, it.receivingDate, it.inspectionDate, "UPDATE"
                    )
                    backlogWhHistoryRepo.save(entityBacklogHistory, it.id)
                }
                ?: throw BusinessExceptionDetail(
                    CommonUtils.getMessage("data.not.found.in.backlog"), "packageCode = $it.packageCode"
                )

        }
    }

    fun getByLocationAndPackageAndPO(locationCode: String, packageCode: String, poNumber: String): BacklogWh {
        return backlogWhRepo.findByLocationAndPackageAndPO(locationCode, packageCode, poNumber)
            ?: throw BusinessExceptionDetail(
                CommonUtils.getMessage("data.not.found.in.backlog"), "locationCode = $locationCode, packageCode = $packageCode, poNumber = $poNumber"
            )
    }

    fun getList(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<BacklogWhResponse> {
        val backlogData = backlogWhRepo.getList(request, pageable)
        val data = backlogData.first
        return BasePagingResponse(
            data,
            backlogData.second
        )
    }

    fun getListForAndroid(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<BacklogWhResponse> {
        val backlogData = backlogWhRepo.getList(request, pageable, fetchAll = true, exactMatch = true)
        return BasePagingResponse(
            backlogData.first,
            backlogData.second
        )
    }

    fun getListForSendingRequest(request: BacklogWhSearchForSendingRequest, pageable: Pageable): BasePagingResponse<BacklogWhForSendingResponse> {
        val backlogData = backlogWhRepo.getListForSendingRequest(request, pageable, fetchAll = true, exactMatch = true)
        val responseData = backlogData.first.map {
            BacklogWhForSendingResponse(
                poNumber = it.poNumber,
                itemCode = it.itemCode,
                productionGroup = it.productionGroup,
                receivingDate = it.receivingDate,
                itemName = it.itemName,
                lotNo = it.lotNo,
                backlogQty = it.backlogQty,
                availableBacklogQty = it.availableBacklogQty,
            )
        }
        return BasePagingResponse(
            responseData,
            backlogData.second
        )
    }

    fun getBacklogHistoryList(packageCode: String): BasePagingResponse<BacklogHistoryResponse>{
        val listBacklogHistoryResponse = backlogWhHistoryRepo.getBacklogHistoryList(packageCode)
        // backlogQty luu theo lot => nhom theo lot, trong moi lot gop cac dong cung loai + cung ngay (giu thu tu thoi gian)
        val data = listBacklogHistoryResponse.first
            .groupBy { it.lotNo }
            .flatMap { (lotNo, histories) ->
                val lotData = mutableListOf<BacklogHistoryResponse>()
                histories.forEach {
                    val receiving = receivingTransRepo.findOneRecordById(it.refId)
                    val sending = sendingTransRepo.findOneRecordById(it.refId)
                    val transactionQty = receiving?.qty ?: sending?.qty ?: BigDecimal.ZERO
                    val last = lotData.lastOrNull()
                    if (last != null && last.transactionType == it.transactionType && last.createDate == it.createdDate?.toLocalDate()) {
                        last.backlogQty = it.backlogQty
                        last.boxQty = it.boxQty
                        last.transactionQty = (last.transactionQty ?: BigDecimal.ZERO).plus(transactionQty)
                    } else {
                        lotData += BacklogHistoryResponse(
                            locationCode = it.locationCode,
                            poNumber = it.poNumber,
                            lotNo = lotNo,
                            inspectionDate = it.inspectionDate,
                            receivingDate = it.receivingDate,
                            transactionQty = transactionQty,
                            backlogQty = it.backlogQty,
                            boxQty = it.boxQty,
                            transactionType = it.transactionType,
                            createDate = it.createdDate?.toLocalDate()
                        )
                    }
                }
                lotData
            }
        return BasePagingResponse(data, data.size)
    }
}