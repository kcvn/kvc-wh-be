package com.kcvn.spm.app.stocktaking.service

import com.kcvn.spm.app.stocktaking.payload.request.*
import com.kcvn.spm.app.stocktaking.payload.response.ActualStockTakingResponse
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingMonthlyResponse
import com.kcvn.spm.app.stocktaking.payload.response.SystemStockTakingResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.pojos.StockTaking
import com.kcvn.spm.model.tables.pojos.StockTakingStatus
import com.kcvn.spm.repository.AmoebaRepository
import com.kcvn.spm.repository.BacklogBinEntryRepository
import com.kcvn.spm.repository.StockTakingRepository
import com.kcvn.spm.repository.StockTakingStatusRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@Service
@Transactional
class StockTakingService(
    private val amoebaRepo: AmoebaRepository,
    private val stockTakingStatusRepo: StockTakingStatusRepository,
    private val stockTakingRepo: StockTakingRepository,
    private val backlogBinEntryRepo: BacklogBinEntryRepository
) {
    fun checkingStartActual(request: StartActualRequest): BaseResponse<String> {
        val stt = stockTakingStatusRepo.findByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
        if (stt != null) {
            if (stt.status == "on-going") {
                return BaseResponse("permit", "${request.monthNumber}/${request.yearNumber} đang kiểm kê. Bạn muốn kiểm kê lại không?")
            } else {
                return BaseResponse("deny", "${request.monthNumber}/${request.yearNumber} đã đóng kiểm kê")
            }
        } else {
            val domain = stockTakingStatusRepo.findByStatus("on-going")
            if (domain != null) {
                return BaseResponse("deny", "${domain.monthNumber}/${domain.yearNumber} đang kiểm kê. Vui lòng đóng trước khi bắt đầu tháng mới")
            } else {
                return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} bắt đầu kiểm kê")
            }
        }
    }

    fun startActual(request: StartActualRequest): BaseResponse<String> {
        val stt = stockTakingStatusRepo.findByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
        if (stt != null) {
            // stock taking again
            stockTakingRepo.deleteByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
            stockTakingRepo.copyFromAmoebaToStockTaking(request)
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} bắt đầu kiểm kê")
        } else {
            // stock taking new
            val sttDomain = StockTakingStatus(
                yearNumber = request.yearNumber,
                monthNumber = request.monthNumber,
                status = "on-going"
            )
            stockTakingStatusRepo.save(sttDomain)
            // copy data from amoeba to stock_taking
            stockTakingRepo.copyFromAmoebaToStockTaking(request)
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} bắt đầu kiểm kê")
        }
    }

    fun scan(request: List<ScanRequest>) {
        val sTT = stockTakingStatusRepo.findByStatus("on-going")
            ?: throw BusinessExceptionDetail(CommonUtils.getMessage("không có tháng nào đang kiểm kê"), "")
        request.forEach { element ->
            val stockTaking = stockTakingRepo.findByInspectionDateAndPO(element.inspectionDate!!, element.poNumber!!)
            if (stockTaking != null) {
                stockTakingRepo.update(sTT.yearNumber!!, sTT.monthNumber!!, element)
            } else {
                val domain = StockTaking(
                    null,
                    sTT.yearNumber,
                    sTT.monthNumber,
                    element.inspectionDate,
                    element.poNumber,
                    "",
                    element.actualLocationCode,
                    null,
                    element.actualQty
                )
                stockTakingRepo.save(domain)
            }
        }
    }

    fun stopActual(request: StopActualRequest) {
        stockTakingStatusRepo.updateStatus(request.yearNumber!!, request.monthNumber!!)
    }

    fun getListSystemStock(request: StockTakingDailyRequest, pageable: Pageable): BasePagingResponse<SystemStockTakingResponse> {
        val stockTakingList = amoebaRepo.getList(request, pageable)
        val systemList = mapToSystemResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            systemList.size
        )
    }

    fun getListActualStock(request: StockTakingMonthlyRequest, pageable: Pageable): BasePagingResponse<ActualStockTakingResponse> {
        val stockTakingList = stockTakingRepo.getList(request, pageable)
        val systemList = mapToActualResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            systemList.size
        )
    }

    fun mapToSystemResponse(input: List<StockTakingDailyResponse>): List<SystemStockTakingResponse> {
        return input.map {
            SystemStockTakingResponse(
                inspectionDate = it.inspectionDate,
                poNumber = it.poNumber,
                amoebaLocationCode = it.amoebaLocationCode,
                systemLocationCode = it.systemLocationCode,
                amoebaQty = it.amoebaQty,
                systemQty = it.systemQty,
                result = resolveResult(it.resultQty, it.resultLocationCode)
            )
        }
    }

    fun mapToActualResponse(input: List<StockTakingMonthlyResponse>): List<ActualStockTakingResponse> {
        return input.map {
            ActualStockTakingResponse(
                inspectionDate = it.inspectionDate,
                poNumber = it.poNumber,
                amoebaLocationCode = it.amoebaLocationCode,
                actualLocationCode = it.actualLocationCode,
                amoebaQty = it.amoebaQty,
                actualQty = it.actualQty,
                result = resolveResult(it.resultQty, it.resultLocationCode)
            )
        }
    }

    fun resolveResult(resultQty: String?, resultLocationCode: String?): String? {
        return when {
            resultQty == "SAME" && resultLocationCode == "SAME" -> "SAME"
            resultQty == "SAME" && resultLocationCode == "DIFFERENT" -> "BIN#: DIFFERENT"
            resultQty == "DIFFERENT" && resultLocationCode == "SAME" -> "QTY: DIFFERENT"
            resultQty == "DIFFERENT" && resultLocationCode == "DIFFERENT" -> "QTY: DIFFERENT, BIN#: DIFFERENT"
            else -> null
        }
    }



    fun importExcel(file: MultipartFile): BaseResponse<Int> {
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportAmoebaTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1
            val headerRow = sheet.getRow(0)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 26))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val amoebaList = mutableListOf<Amoeba>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val amoebaData = Amoeba(
                    inspectionDate = ExcelHelper.getCellValueDate(row, 8),
                    poNumber = ExcelHelper.getCellValueAmoeba(row, 10),
                    locationCode = ExcelHelper.getCellValueAmoeba(row, 19),
                    qty = ExcelHelper.getCellValueAmoeba(row, 11).toBigDecimalOrNull() ?: BigDecimal.ZERO
                )

                amoebaList.add(amoebaData)
            }
            // xóa record amoeba
            amoebaRepo.delete()
            // save amoeba
            val totalRecord = amoebaRepo.saveAll(amoebaList)
            // xóa record backlog bin entry
            backlogBinEntryRepo.delete()
            // save backlog bin entry
            val binEntryList = backlogBinEntryRepo.getBinEntryFromBacklog()
            backlogBinEntryRepo.saveAll(binEntryList.first)

            return BaseResponse(totalRecord, CommonUtils.getMessage("Inserted"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }
}