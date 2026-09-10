package com.kcvn.spm.app.purchaseOrderBacklog.service

import com.kcvn.spm.app.purchaseOrderBacklog.payload.response.PurchaseOrderBacklogResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.repository.PurchaseOrderBacklogRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class PurchaseOrderBacklogService(private val purchaseOrderBacklogRepo: PurchaseOrderBacklogRepository) {
    fun importPurchaseOrderBacklog(file: MultipartFile): BaseResponse<Int> {
        //val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/TempCheckingImportedTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 3
//            val headerRow = sheet.getRow(4)
//            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 4, 4))
//                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val errorList = mutableListOf<PurchaseOrderBacklog>()
            var count = 0

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val seqNo = ExcelHelper.getCellValueAmoeba(row, 0).toBigDecimal()
                val poNo = ExcelHelper.getCellValueAmoeba(row, 28)
                val invoice = ExcelHelper.getCellValueAmoeba(row, 1)
                val productionGroup = ExcelHelper.getCellValueAmoeba(row, 12)
                val productionGroupCode = when (productionGroup) {
                    "2317" -> "C"
                    "2311" -> "D"
                    "2314" -> "E"
                    "2315" -> "F"
                    "2318" -> "G"
                    "2316" -> "H"
                    "2312" -> "I"
                    "2313" -> "J"
                    else -> "Z"
                }

                val lotNo = generateLotNo(seqNo, invoice, poNo, productionGroupCode)

                val data = PurchaseOrderBacklog(
                    seqNo = seqNo,
                    orderDate = ExcelHelper.getCellValueDate(row, 5),
                    itemCode = ExcelHelper.getCellValueAmoeba(row, 6),
                    itemName = ExcelHelper.getCellValueAmoeba(row, 7),
                    prodGroup = productionGroup,
                    storageLocation = ExcelHelper.getCellValueAmoeba(row, 13),
                    orderQty = ExcelHelper.getCellValueAmoeba(row, 14).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    unit = ExcelHelper.getCellValueAmoeba(row, 15),
                    poNo = poNo,
                    invoice = invoice,
                    detail = ExcelHelper.getCellValueAmoeba(row, 29),
                    itemType = ExcelHelper.getCellValueAmoeba(row, 63),
                    lotNo = lotNo.second
                )
                if (lotNo.first == "Insert") {
                    purchaseOrderBacklogRepo.save(data)
                    count++
                }
                else if (lotNo.first == "Update") {
                    purchaseOrderBacklogRepo.update(data)
                    count++
                }
            }
            return BaseResponse(count, CommonUtils.getMessage("action.succeeded"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun generateLotNo(seqNo: BigDecimal, invoice: String, poNo: String, productionGroup: String): Pair<String,String> {
        val currentDate = OffsetDateTime.now().toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyMMdd"))
        val latestLot = purchaseOrderBacklogRepo.findLatestLotOfDay(currentDate, productionGroup)
        val existedLot = purchaseOrderBacklogRepo.findLotOfPoInvoiceOnDay(seqNo, poNo, invoice, currentDate)

        if (latestLot == null) //neu la lot dau cua ngay cua 1 bo phan thi mac dinh la 000
             return "Insert" to "$productionGroup${currentDate}000"
        else if (existedLot == null) //neu khong phai lot dau cua ngay va PO-Invoice do chua ton tai thi phai tang seq
            {
                val nextSequence = latestLot?.lotNo!!.takeLast(3).toInt() + 1
                return "Insert" to "$productionGroup$currentDate${nextSequence.toString().padStart(3, '0')}"
            }
        else return "Update" to existedLot.lotNo.toString() //neu PO-Invoice da ton tai trong ngay do thi tra ve lot da ton tai
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/TempCheckingImportedTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.use { it.write(byteArrayOutputStream) }

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("TempCheckingImportedTemplate.xlsx"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = byteArrayOutputStream.toByteArray()
        )

        return BaseResponse(response)
    }

    fun getListForDropDown(status: String, isIncludeGe1Days: Boolean): BaseResponse<List<DropdownResponse>> {
        val listOrder = purchaseOrderBacklogRepo.getListForDropDown(status, isIncludeGe1Days)
        val dropDownList= listOrder.map { order ->
            DropdownResponse(
                "${order.invoice} ~ ${
                    order.createdDate?.toLocalDate()}",
                "${order.invoice} ~ ${
                    order.createdDate?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            )
        }.toMutableList()
        return BaseResponse(data = dropDownList)
    }

    fun getListByInvoiceSeqNoAndDate(invoiceNumber: String, createdDate: LocalDate, status: String): List<PurchaseOrderBacklogResponse> {
        val listOrder = purchaseOrderBacklogRepo.getListByInvoiceSeqNoAndDate(invoiceNumber, createdDate, status)
        return listOrder.map { PurchaseOrderBacklogResponse(
            seqNo = it.seqNo?.toInt() ?: 0,
            orderDate = it.orderDate ?: LocalDate.MIN,
            itemCode = it.itemCode.orEmpty(),
            itemName = it.itemName.orEmpty(),
            prodGroup = it.prodGroup.orEmpty(),
            storageLocation = it.storageLocation.orEmpty(),
            orderQty = it.orderQty ?: BigDecimal.ZERO,
            unit = it.unit.orEmpty(),
            poNumber = it.poNo.orEmpty(),
            invoiceNumber = it.invoice.orEmpty(),
            detail = it.detail.orEmpty(),
            itemType = it.itemType.orEmpty(),
            lotNo = it.lotNo.orEmpty(),
        ) }
    }
}