package com.kcvn.spm.app.backlogwh.service

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.BacklogWhHistory
import com.kcvn.spm.repository.BacklogWhHistoryRepository
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.SplittingRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class BacklogWhService(
    private val backlogWhRepo: BacklogWhRepository,
    private val backlogWhHistoryRepo: BacklogWhHistoryRepository,
    private val splittingRepo: SplittingRepository
) {
    fun exportBacklogWhExcel(request: BacklogWhSearchRequest, pageable: Pageable): BaseResponse<FileContentModel> {
        val listBacklogResponse = backlogWhRepo.getList(request, pageable, true)

        val inputStream = this::class.java.classLoader.getResourceAsStream("assets/template/ExportBacklogWhTemplate.xlsx")
            ?: throw FileNotFoundException("ExportBacklogWhTemplate.xlsx file not found in resources.")
        val workbook = XSSFWorkbook(inputStream)
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
            ExcelHelper.setCellValue(row, 0, style, item.locationCode)
            ExcelHelper.setCellValue(row, 1, style, item.poNumber)
            ExcelHelper.setCellValueInt(row, 2, numberStyle, item.backlogQty?.toInt() ?: 0, numberFormat)

            val formattedDate = item.receivingDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: ""
            ExcelHelper.setCellValue(row, 3, style, formattedDate)
        }

        sheet.createFreezePane(4, 1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportBacklog", arrayOf(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
            )),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }


    fun plusBacklog(data: BacklogWh, transactionType: String) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
        // get receiving date
        val splitting = splittingRepo.findByLocationAndPackage(data.locationCode!!, data.packageCode!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val receivingDate = splitting.receivingDate
        if (backlog == null) {
            val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty, data.boxQty, receivingDate)
            backlogWhRepo.save(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty, data.boxQty, receivingDate, null, transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory)
        } else {
            val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!),
                data.boxQty?.plus(backlog.boxQty!!)
            )
            backlogWhRepo.update(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!), data.boxQty?.plus(backlog.boxQty!!), receivingDate, null, transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory)
        }
    }

    fun minusBacklog(data: BacklogWh, transactionType: String) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, backlog.backlogQty?.minus(data.backlogQty!!),
            backlog.boxQty?.minus(data.boxQty!!)
        )
        backlogWhRepo.update(entityBacklog)
        // insert backlog history
        val entityBacklogHistory = BacklogWhHistory(
            null, entityBacklog.locationCode, entityBacklog.poNumber, entityBacklog.packageCode, entityBacklog.backlogQty, entityBacklog.boxQty, data.receivingDate, null, transactionType
        )
        backlogWhHistoryRepo.save(entityBacklogHistory)
    }

    fun getByLocationAndPackageAndPO(locationCode: String, packageCode: String, poNumber: String): BacklogWh {
        return backlogWhRepo.findByLocationAndPackageAndPO(locationCode, packageCode, poNumber)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
    }

    fun getList(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<BacklogWhResponse> {
        val backlogData = backlogWhRepo.getList(request, pageable)
        val data = backlogData.first.map {
            BacklogWhResponse(
                locationCode = it.locationCode,
                poNumber = it.poNumber,
                packageCode = it.packageCode,
                backlogQty = it.backlogQty,
                boxQty = it.boxQty,
                receivingDate = it.receivingDate,
                issueDate = it.issueDate
            )
        }
        return BasePagingResponse(
            data,
            backlogData.second
        )
    }
}