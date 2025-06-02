package com.kcvn.spm.app.stocktaking.service

import com.kcvn.spm.app.stocktaking.payload.request.*
import com.kcvn.spm.app.stocktaking.payload.response.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
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
    fun getListForAndroid(pageable: Pageable): BasePagingResponse<StockTakingForAndroid> {
        val sTT = stockTakingStatusRepo.findByStatus("on-going")
            ?: throw BusinessExceptionDetail(CommonUtils.getMessage("no.months.taking.inventory"), "")
        val stockTakingData = stockTakingRepo.getListForAndroid(sTT.yearNumber!!, sTT.monthNumber!!, pageable)
        val data = stockTakingData.first.map {
            StockTakingForAndroid(
                inspectionDate = it.inspectionDate,
                poNumber = it.poNumber,
                amoebaLocationCode = it.amoebaLocationCode,
                actualLocationCode = it.actualLocationCode,
                amoebaQty = it.amoebaQty,
                actualQty = it.actualQty
            )
        }
        return BasePagingResponse(
            data,
            stockTakingData.second
        )
    }

    fun checkingStartActual(request: StartActualRequest): BaseResponse<String> {
        val stt = stockTakingStatusRepo.findByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
        if (stt != null) {
            if (stt.status == "on-going") {
                return BaseResponse("permit", "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("taking.inventory") + ". " + CommonUtils.getMessage("check.again"))
            } else {
                return BaseResponse("deny", "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("closed.inventory"))
            }
        } else {
            val domain = stockTakingStatusRepo.findByStatus("on-going")
            if (domain != null) {
                return BaseResponse("deny", "${domain.monthNumber}/${domain.yearNumber} " + CommonUtils.getMessage("taking.inventory") + ". " + CommonUtils.getMessage("close.before.start.new.month"))
            } else {
                return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("start.inventory"))
            }
        }
    }

    fun startActual(request: StartActualRequest): BaseResponse<String> {
        val stt = stockTakingStatusRepo.findByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
        if (stt != null) {
            // stock taking again
            stockTakingRepo.deleteByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
            stockTakingRepo.copyFromAmoebaToStockTaking(request)
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("start.inventory"))
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
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("start.inventory"))
        }
    }

    fun scan(request: List<ScanRequest>) {
        val sTT = stockTakingStatusRepo.findByStatus("on-going")
            ?: throw BusinessExceptionDetail(CommonUtils.getMessage("no.months.taking.inventory"), "")
        request.forEach { element ->
            val inspectionDate = element.inspectionDate
            val poNumber = element.poNumber
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

            if (inspectionDate == null || poNumber == null) {
                stockTakingRepo.save(domain)
                return@forEach
            }
            val stockTaking = stockTakingRepo.findByInspectionDateAndPO(inspectionDate, poNumber)
            if (stockTaking != null) {
                stockTakingRepo.update(sTT.yearNumber!!, sTT.monthNumber!!, element)
            } else {
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
            stockTakingList.second
        )
    }

    fun getListActualStock(request: StockTakingMonthlyRequest, pageable: Pageable): BasePagingResponse<ActualStockTakingResponse> {
        val stockTakingList = stockTakingRepo.getList(request, pageable)
        val systemList = mapToActualResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            stockTakingList.second
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

    fun importTXTAmoeba(file: MultipartFile): BaseResponse<Int> {
        val amoebaList = mutableListOf<Amoeba>()
        try {
            if (!file.originalFilename.orEmpty().lowercase().endsWith(".txt")) {
                throw BusinessException(CommonUtils.getMessage("validate.invalidFormatTXT"))
            }
            val lines = file.inputStream.bufferedReader().readLines()
            if (lines.isEmpty() || lines.size <= 1) {
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            }
            val header = lines[0].split("\t")
            if (header.size != 26) {
                throw BusinessException(CommonUtils.getMessage("validate.invalidFormat"))
            }
            for (i in 1 until lines.size) {
                val columns = lines[i].split("\t")

                if (columns.size < 26) continue

                val amoebaData = Amoeba(
                    inspectionDate = CommonUtils.parseDate(columns[8]),
                    poNumber = columns[10].trim(),
                    locationCode = columns[19].trim(),
                    qty = columns[11].trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
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

            return BaseResponse(totalRecord, CommonUtils.getMessage("action.succeeded"))
        } catch (e: Exception) {
            throw e
        }
    }
}