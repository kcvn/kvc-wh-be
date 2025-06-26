package com.kcvn.spm.app.stocktaking.service

import com.kcvn.spm.app.stocktaking.payload.request.*
import com.kcvn.spm.app.stocktaking.payload.response.*
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.pojos.StockTaking
import com.kcvn.spm.model.tables.pojos.StockTakingStatus
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
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
import java.time.format.DateTimeFormatter

@Service
@Transactional
class StockTakingService(
    private val amoebaRepo: AmoebaRepository,
    private val stockTakingStatusRepo: StockTakingStatusRepository,
    private val stockTakingRepo: StockTakingRepository,
    private val backlogBinEntryRepo: BacklogBinEntryRepository,
    private val backlogWhRepo: BacklogWhRepository,
) {
    fun getListForAndroid(pageable: Pageable): BasePagingResponse<StockTakingForAndroid> {
        val sTT = stockTakingStatusRepo.findByStatus("on-going")
            ?: throw BusinessExceptionDetail(CommonUtils.getMessage("no.months.taking.inventory"), "")
        val stockTakingData = stockTakingRepo.getListForAndroid(sTT.yearNumber!!, sTT.monthNumber!!, pageable)
        val data = stockTakingData.first.map {
            StockTakingForAndroid(
                poNumber = it.poNumber,
                packageCode = it.packageCode,
                systemLocationCode = it.systemLocationCode,
                actualLocationCode = it.actualLocationCode,
                systemQty = it.systemQty,
                actualQty = it.actualQty,
                systemBoxQty = it.systemBoxQty,
                actualBoxQty = it.actualBoxQty,
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
            stockTakingRepo.copyFromBacklogWhToStockTaking(request)
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("start.inventory"))
        } else {
            // stock taking new
            val sttDomain = StockTakingStatus(
                yearNumber = request.yearNumber,
                monthNumber = request.monthNumber,
                status = "on-going"
            )
            stockTakingStatusRepo.save(sttDomain)
            // copy data from backlog_wh to stock_taking
            stockTakingRepo.copyFromBacklogWhToStockTaking(request)
            return BaseResponse(null, "${request.monthNumber}/${request.yearNumber} " + CommonUtils.getMessage("start.inventory"))
        }
    }

    fun scan(request: List<ScanRequest>) {
        val sTT = stockTakingStatusRepo.findByStatus("on-going")
            ?: throw BusinessExceptionDetail(CommonUtils.getMessage("no.months.taking.inventory"), "")
        request.forEach { element ->
            val backlog = backlogWhRepo.findByPackageCode(element.packageCode!!)
                ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.backlog"), "packageCode = ${element.packageCode}")
            val poNumber = backlog.poNumber
            val domain = StockTaking(
                null,
                sTT.yearNumber,
                sTT.monthNumber,
                poNumber,
                element.packageCode,
                null,
                element.actualLocationCode,
                null,
                element.actualQty,
                null,
                element.actualBoxQty
            )

            val stockTaking = stockTakingRepo.findByPackageCode(element.packageCode!!)
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

    fun getListSystemStockByRawSql(request: StockTakingDailyRequest, pageable: Pageable): BasePagingResponse<SystemStockTakingResponse> {
        val stockTakingList = amoebaRepo.getListByRawSql(request, pageable)
        val systemList = mapToSystemResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            stockTakingList.second
        )
    }

    fun getAllListSystemStockForExport(request: StockTakingDailyRequest): BasePagingResponse<SystemStockTakingResponse> {
        val stockTakingList = amoebaRepo.getAll(request)
        val systemList = mapToSystemResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            stockTakingList.second
        )
    }

    fun getListActualStockByRawSql(request: StockTakingMonthlyRequest, pageable: Pageable): BasePagingResponse<ActualStockTakingResponse> {
        val stockTakingList = stockTakingRepo.getListByRawSql(request, pageable)
        val systemList = mapToActualResponse(stockTakingList.first)
        return BasePagingResponse(
            systemList,
            stockTakingList.second
        )
    }

    fun getAllListActualStockForExport(request: StockTakingMonthlyRequest): BasePagingResponse<ActualStockTakingResponse> {
        val stockTakingList = stockTakingRepo.getAll(request)
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
                poNumber = it.poNumber,
                packageCode = it.packageCode,
                systemLocationCode = it.systemLocationCode,
                actualLocationCode = it.actualLocationCode,
                systemQty = it.systemQty,
                actualQty = it.actualQty,
                systemBoxQty = it.systemBoxQty,
                actualBoxQty = it.actualBoxQty,
                result = resolveResult(it.resultLocationCode, it.resultQty, it.resultBoxQty)
            )
        }
    }

    fun resolveResult(resultQty: String?, resultLocationCode: String?): String? {
        val results = mutableListOf<String>()

        if (resultQty == "DIFFERENT") results.add("QTY: DIFFERENT")
        if (resultLocationCode == "DIFFERENT") results.add("BIN#: DIFFERENT")

        return when {
            results.isEmpty() -> "SAME"
            else -> results.joinToString(", ")
        }
    }

    fun resolveResult(resultLocationCode: String?, resultQty: String?, resultBoxQty: String?): String? {
        val results = mutableListOf<String>()

        if (resultLocationCode == "DIFFERENT") results.add("BIN#: DIFFERENT")
        if (resultQty == "DIFFERENT") results.add("QTY: DIFFERENT")
        if (resultBoxQty == "DIFFERENT") results.add("BOX QTY: DIFFERENT")

        return when {
            results.isEmpty() -> "SAME"
            else -> results.joinToString(", ")
        }
    }

    fun exportSystemStockTaking(request: StockTakingDailyRequest): BaseResponse<FileContentModel> {
        val listSystemStockTakingResponse = getAllListSystemStockForExport(request)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportSystemStockTakingTemplate.xlsx")
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

        val listSystemStockTaking = listSystemStockTakingResponse.data

        if (listSystemStockTaking.isNullOrEmpty()) {
            throw BusinessException(CommonUtils.getMessage("data.notFound"))
        }

        var rowNumberFill = 1
        for (item in listSystemStockTaking) {
            val row: Row = sheet.createRow(rowNumberFill++)

            val formattedInspectionDate = item.inspectionDate?.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) ?: ""
            ExcelHelper.setCellValue(row, 0, style, formattedInspectionDate)
            ExcelHelper.setCellValue(row, 1, style, item.poNumber)
            ExcelHelper.setCellValue(row, 2, style, item.amoebaLocationCode)
            ExcelHelper.setCellValue(row, 3, style, item.systemLocationCode)
            ExcelHelper.setCellValueInt(row, 4, numberStyle, item.amoebaQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValueInt(row, 5, numberStyle, item.systemQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValue(row, 6, style, item.result)
        }

        sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ExportSystemStockTaking.xlsx", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun exportActualStockTaking(request: StockTakingMonthlyRequest): BaseResponse<FileContentModel> {
        val listActualStockTakingResponse = getAllListActualStockForExport(request)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportActualStockTakingTemplate.xlsx")
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

        val listActualStockTaking = listActualStockTakingResponse.data

        if (listActualStockTaking.isNullOrEmpty()) {
            throw BusinessException(CommonUtils.getMessage("data.notFound"))
        }

        var rowNumberFill = 1
        for (item in listActualStockTaking) {
            val row: Row = sheet.createRow(rowNumberFill++)

            ExcelHelper.setCellValue(row, 0, style, item.poNumber)
            ExcelHelper.setCellValue(row, 1, style, item.packageCode)
            ExcelHelper.setCellValue(row, 2, style, item.systemLocationCode)
            ExcelHelper.setCellValue(row, 3, style, item.actualLocationCode)
            ExcelHelper.setCellValueInt(row, 4, numberStyle, item.systemQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValueInt(row, 5, numberStyle, item.actualQty?.toInt() ?: 0, numberFormat)
            ExcelHelper.setCellValueInt(row, 6, numberStyle, item.systemBoxQty ?: 0, numberFormat)
            ExcelHelper.setCellValueInt(row, 7, numberStyle, item.actualBoxQty ?: 0, numberFormat)
            ExcelHelper.setCellValue(row, 8, style, item.result)
        }

        sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("ExportActualStockTaking.xlsx", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
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
                    inspectionDate = CommonUtils.parseDateAmoeba(columns[8]),
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