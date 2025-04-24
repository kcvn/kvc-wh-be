package com.kcvn.spm.app.stocktaking.service

import com.kcvn.spm.app.stocktaking.payload.request.StartActualRequest
import com.kcvn.spm.app.stocktaking.payload.request.StockTakingDailyRequest
import com.kcvn.spm.app.stocktaking.payload.request.StopActualRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.pojos.StockTakingStatus
import com.kcvn.spm.repository.AmoebaRepository
import com.kcvn.spm.repository.StockTakingRepository
import com.kcvn.spm.repository.StockTakingStatusRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileNotFoundException
import java.math.BigDecimal

@Service
@Transactional
class StockTakingService(
    private val amoebaRepo: AmoebaRepository,
    private val stockTakingStatusRepo: StockTakingStatusRepository,
    private val stockTakingRepo: StockTakingRepository
) {
    fun startActual(request: StartActualRequest): BaseResponse<String> {
        val stt = stockTakingStatusRepo.findByYearAndMonth(request.yearNumber!!, request.monthNumber!!)
        if (stt != null) {
            if (stt.status == "on-going") {
                return BaseResponse("on-going", "${request.yearNumber}/${request.yearNumber} đang kiểm kê")
            } else {
                return BaseResponse("completed", "${request.yearNumber}/${request.yearNumber} đã đóng kiểm kê")
            }
        } else {
            val domain = stockTakingStatusRepo.findByStatus("on-going")
            if (domain != null) {
                return BaseResponse("on-going", "${domain.yearNumber}/${domain.yearNumber} đang kiểm kê")
            } else {
                // insert stock_taking_status
                val sttDomain = StockTakingStatus(
                    yearNumber = request.yearNumber,
                    monthNumber = request.monthNumber,
                    status = "on-going"
                )
                stockTakingStatusRepo.save(sttDomain)
                // copy data from amoeba to stock_taking
                stockTakingRepo.copyFromAmoebaToStockTaking(request)
                return BaseResponse(null, "${request.yearNumber}/${request.yearNumber} bắt đầu kiểm kê")
            }
        }
    }

    fun stopActual(request: StopActualRequest) {
        stockTakingStatusRepo.updateStatus(request.yearNumber!!, request.monthNumber!!)
    }

    fun getList(request: StockTakingDailyRequest, pageable: Pageable): BasePagingResponse<StockTakingDailyResponse> {
        val data = amoebaRepo.getList(request, pageable)
        return BasePagingResponse(
            data.first,
            data.second
        )
    }

    fun importExcel(file: MultipartFile): BaseResponse<Int> {
        val templateStream = this::class.java.classLoader.getResourceAsStream("assets/template/ImportAmoebaTemplate.xlsx")
            ?: throw FileNotFoundException("ImportAmoebaTemplate.xlsx file not found in resources.")
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1
            val headerRow = sheet.getRow(0)
            // Tạo file tạm thời từ InputStream
            val tempFile = File.createTempFile("ImportAmoebaTemplate", ".xlsx").apply {
                deleteOnExit()
                outputStream().use { templateStream.copyTo(it) }
            }
            if (!ExcelHelper.columnIsMatchingTemplate(tempFile.absolutePath, headerRow, 0, 26))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val amoebaList = mutableListOf<Amoeba>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val amoebaData = Amoeba(
                    locationCode = ExcelHelper.getCellValueAmoeba(row, 19),
                    poNumber = ExcelHelper.getCellValueAmoeba(row, 10),
                    qty = ExcelHelper.getCellValueAmoeba(row, 11).toBigDecimalOrNull() ?: BigDecimal.ZERO
                )

                amoebaList.add(amoebaData)
            }
            // xóa record
            amoebaRepo.delete()
            val totalRecord = amoebaRepo.saveAll(amoebaList)
            return BaseResponse(totalRecord, CommonUtils.getMessage("Inserted"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }
}