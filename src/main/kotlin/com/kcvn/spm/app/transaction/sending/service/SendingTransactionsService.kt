package com.kcvn.spm.app.transaction.sending.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.transaction.sending.payload.request.*
import com.kcvn.spm.app.transaction.sending.payload.response.SendingResponse
import com.kcvn.spm.app.transaction.sending.payload.response.TempSendingInquiryResponse
import com.kcvn.spm.app.transaction.sending.payload.response.ValidateSendTransResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.TempSendingCheckingTransactions
import com.kcvn.spm.model.tables.pojos.TempSendingTransactions
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class SendingTransactionsService(
    private val sendingRepo: SendingTransactionsRepository,
    private val tempSendingRepo: TempSendingTransactionsRepository,
    private val tempSendingImportedRepo: TempSendingImportedRepository,
    private val tempSendingCheckingRepo: TempSendingCheckingTransactionsRepository,
    private val backlogWhService: BacklogWhService,
    private val backlogWhRepository: BacklogWhRepository
) {
    fun getList(request: SendingSearchRequest, pageable: Pageable): BasePagingResponse<SendingResponse> {
        val moving = sendingRepo.getList(request, pageable)
        val data = moving.first.map {
            SendingResponse(
                sourceLocationCode = it.sourceLocationCode,
                destLocationCode = it.destLocationCode,
                poNumber = it.poNumber,
                qty = it.qty,
                seq = it.seqNo,
                createdDate = it.createdDate
            )
        }
        return BasePagingResponse(
            data,
            moving.second
        )
    }

    fun getTempSendingList(formCode: String, pageable: Pageable):BasePagingResponse<TempSendingInquiryResponse>{
        val data = tempSendingRepo.getListForApprove(formCode, pageable)
        return BasePagingResponse(
            data.first,
            data.second
        )
    }

    fun approveSendingForm(formCode: String){
        sendingRepo.copyToSendingTable(formCode)
        tempSendingImportedRepo.updateAfterApprove(formCode)
        val sendingList = tempSendingRepo.getListByFormCode(formCode)
        sendingList?.forEach {
            val backlog = backlogWhRepository.findByLocationAndPackageAndPO(it.sourceLocationCode!!, it.sourcePackageCode!!, it.poNumber!!)
            if (backlog == null || backlog.backlogQty!! < it.qty) throw BusinessExceptionDetail(
                CommonUtils.getMessage("not.enough.backlog"), "locationCode = ${it.sourceLocationCode}, packageCode = ${it.sourcePackageCode}"
            )
            val boxQty: Int = if (it.notMinusBoxQty == true) {
                0
            } else {
                1
            }
            val backlogData = BacklogWh(
                null,
                it.sourceLocationCode,
                it.poNumber,
                it.sourcePackageCode,
                it.qty,
                boxQty,
                backlog.receivingDate,
                it.inspectionDate,
                isEntried = true
            )
            backlogWhService.minusBacklog(backlogData, "OUT_ONLY")
        }
    }

    fun exportExcel(pageable: Pageable): BaseResponse<FileContentModel> {
        val listBacklogResponse = sendingRepo.getExcelList(pageable)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportSendingTemplate.xlsx")
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

            val formattedDate = item.inspectionDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) ?: ""
            ExcelHelper.setCellValue(row, 0, style, formattedDate)
            ExcelHelper.setCellValue(row, 1, style, item.poNumber)
            ExcelHelper.setCellValueInt(row, 3, numberStyle, item.qty?.toInt() ?: 0, numberFormat)
        }

        for (i in 1 until rowNumberFill) {
            val row = sheet.getRow(i) ?: sheet.createRow(i)
            val formulaCell = row.createCell(2, CellType.FORMULA)
            formulaCell.cellFormula = "TEXT(D${i + 1},\"#,##0.0\")"
            formulaCell.cellStyle = numberStyle
        }
        sheet.forceFormulaRecalculation = true

        sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ExportSending.xlsx", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun importExcel(file: MultipartFile): BaseResponse<List<ImportSending>> {
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportSendingTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1
            val headerRow = sheet.getRow(0)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 4))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val updatedList = mutableListOf<ImportSending>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val status = ExcelHelper.getCellValue(row, 3)
                if (status != "Success") continue

                val importSendingData = ImportSending(
                    inspectionDate = ExcelHelper.getCellValueDateAmoeba(row, 0),
                    poNumber = ExcelHelper.getCellValueAmoeba(row, 1),
                    qty = ExcelHelper.getCellValueAmoeba(row, 2).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                )

                val isSuccess = sendingRepo.updateIsUpdatedAmoeba(importSendingData)
                if (isSuccess) {
                    updatedList.add(importSendingData)
                }
            }
            return BaseResponse(updatedList, CommonUtils.getMessage("Updated"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun validateSourceBacklogFromSending(request: List<SendingRequest>): List<ValidateSendTransResponse> {
        val list = aggregateSendingRequests(request)
        val response = mutableListOf<ValidateSendTransResponse>()
        list.forEach {
            val backlog = backlogWhService.getByLocationAndPackageAndPO(it.sourceLocationCode!!, it.sourcePackageCode!!, it.poNumber!!)
            if (it.qty!! > backlog.backlogQty!!) {
                val vmr = ValidateSendTransResponse(
                    sourceLocationCode = it.sourceLocationCode,
                    sourcePackageCode = it.sourcePackageCode,
                    poNumber = it.poNumber
                )
                response.add(vmr)
            }
        }
        return response
    }

    fun aggregateSendingRequests(sendingRequests: List<SendingRequest>): List<ValidateSendTransRequest> {
        return sendingRequests
            .groupBy { Triple(it.locationCode, it.packageCode, it.poNumber) }
            .map { (key, group) ->
                ValidateSendTransRequest(
                    sourceLocationCode = key.first,
                    sourcePackageCode = key.second,
                    poNumber = key.third,
                    qty = group.sumOf { it.qty ?: BigDecimal.ZERO }
                )
            }
    }

    fun saveSendTrans(request: List<SendingRequest>) {
        val removeList = request
            .map { Triple(it.formCode, it.poNumber, it.inspectionDate) }
            .distinct()
        removeList.forEach { (formCode, poNumber, inspectionDate) ->
            tempSendingRepo.deleteSendingTrans(formCode, poNumber, inspectionDate)
        }
        val list = createSendTransRequestWithSeq(request)

        list.forEach {
            // get receivingDate
            val backlog = backlogWhRepository.findByLocationAndPackageAndPO(it.sourceLocationCode!!, it.packageCode!!, it.poNumber!!)
            if (backlog == null || backlog.backlogQty!! < it.qty) throw BusinessExceptionDetail(
                    CommonUtils.getMessage("not.enough.backlog"), "locationCode = ${it.sourceLocationCode}, packageCode = ${it.packageCode}"
                )
            val receivingDate = backlog.receivingDate
//            val sendTran = SendingTransactions(
//                null,
//                it.sourceLocationCode,
//                "KVC",
//                it.packageCode,
//                it.packageCode,
//                it.poNumber,
//                it.qty,
//                it.seqNo,
//                "OUT_ONLY",
//                receivingDate,
//                it.inspectionDate,
//            )
            val tempSendTran = TempSendingTransactions(
                null,
                it.formCode,
                it.sourceLocationCode,
                "KVC",
                it.packageCode,
                it.packageCode,
                it.poNumber,
                it.qty,
                it.seqNo,
                "OUT_ONLY",
                receivingDate,
                it.inspectionDate,
                notMinusBoxQty = it.notMinusBoxQty,
                minBinCode = it.minBinCode
            )
            //sendingRepo.saveSendingTrans(sendTran)
            tempSendingRepo.saveTempSendingTrans(tempSendTran)
            // save backlog and backlog history
//            val boxQty: Int = if (it.notMinusBoxQty == true) {
//                0
//            } else {
//                1
//            }
//            val backlogData = BacklogWh(
//                null,
//                it.sourceLocationCode,
//                it.poNumber,
//                it.packageCode,
//                it.qty,
//                boxQty,
//                receivingDate,
//                it.inspectionDate,
//                isEntried = true
//            )
//            backlogWhService.minusBacklog(backlogData, "OUT_ONLY")
        }
    }

    fun saveSendCheckingTrans(request: List<SendingRequest>) {
        val removeList = request
            .map { Triple(it.formCode, it.poNumber, it.inspectionDate) }
            .distinct()
        removeList.forEach { (formCode, poNumber, inspectionDate) ->
            tempSendingCheckingRepo.deleteSendingTrans(formCode, poNumber, inspectionDate)
        }

        request.forEach {
            // get receivingDate
            //val backlog = backlogWhRepository.findByLocationAndPackageAndPO(it.sourceLocationCode!!, it.packageCode!!, it.poNumber!!)
            //val receivingDate = backlog?.receivingDate
            val tempSendTran = TempSendingCheckingTransactions(
                null,
                formCode = it.formCode,
                sourceLocationCode = it.locationCode,
                poNumber = it.poNumber,
                qty = it.qty,
                inspectionDate = it.inspectionDate
            )
            tempSendingCheckingRepo.saveTempSendingCheckingTrans(tempSendTran)
        }
    }

    fun createSendTransRequestWithSeq(requests: List<SendingRequest>): List<SendTransRequestWithSeq> {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        return requests
            .groupBy { Triple(it.locationCode, it.packageCode, it.poNumber) }
            .flatMap { (key, group) ->
                val (sourceLocationCode, sourcePackageCode, poNumber) = key
                val latestSeqNo = sendingRepo.findLatestSending(sourceLocationCode!!, sourcePackageCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, sendTransRequest ->
                    SendTransRequestWithSeq(
                        formCode = sendTransRequest.formCode,
                        inspectionDate = sendTransRequest.inspectionDate,
                        sourceLocationCode = sendTransRequest.locationCode,
                        packageCode = sendTransRequest.packageCode,
                        poNumber = sendTransRequest.poNumber,
                        qty = sendTransRequest.qty,
                        notMinusBoxQty = sendTransRequest.notMinusBoxQty,
                        seqNo = latestSeqNo + index + 1, // Bắt đầu từ latestSeqNo + 1, tăng dần
                        minBinCode = sendTransRequest.minBinCode
                    )
                }
            }
    }

    fun createSendCheckingTransRequestWithSeq(requests: List<SendingRequest>): List<SendTransRequestWithSeq> {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        return requests
            .groupBy { Triple(it.locationCode, it.packageCode, it.poNumber) }
            .flatMap { (key, group) ->
                val (sourceLocationCode, sourcePackageCode, poNumber) = key
                val latestSeqNo = sendingRepo.findLatestSending(sourceLocationCode!!, sourcePackageCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, sendTransRequest ->
                    SendTransRequestWithSeq(
                        formCode = sendTransRequest.formCode,
                        inspectionDate = sendTransRequest.inspectionDate,
                        sourceLocationCode = sendTransRequest.locationCode,
                        packageCode = sendTransRequest.packageCode,
                        poNumber = sendTransRequest.poNumber,
                        qty = sendTransRequest.qty,
                        notMinusBoxQty = sendTransRequest.notMinusBoxQty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }

}