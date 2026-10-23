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
            val rowIndex = 1
//            val headerRow = sheet.getRow(4)
//            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 4, 4))
//                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val errorList = mutableListOf<PurchaseOrderBacklog>()
            var count = 0

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val seqNo = ExcelHelper.getCellValueAmoeba(row, 0).toBigDecimal()
                val poNo = ExcelHelper.getCellValueAmoeba(row, 27)
                val invoice = ExcelHelper.getCellValueAmoeba(row, 1)
                val itemType = ExcelHelper.getCellValueAmoeba(row, 62)
                val productionGroup = ExcelHelper.getCellValueAmoeba(row, 11)
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
                val convertedProductionGroup = when (productionGroup){
                    "2317" -> "CMOS"
                    "2311" -> "SMD"
                    "2314" -> "META"
                    "2315" -> "CPD"
                    "2318" -> "RFD"
                    "2316" -> "ITD"
                    "2312" -> "PMD"
                    "2313" -> "JMD"
                    else -> "ADM"
                }

                val lotNo = generateLotNo(seqNo, invoice, poNo, productionGroupCode)

                val data = PurchaseOrderBacklog(
                    seqNo = seqNo,
                    orderDate = ExcelHelper.getCellValueDate(row, 4),
                    itemCode = ExcelHelper.getCellValueAmoeba(row, 5),
                    itemName = ExcelHelper.getCellValueAmoeba(row, 6),
                    prodGroup = convertedProductionGroup,
                    storageLocation = ExcelHelper.getCellValueAmoeba(row, 12),
                    orderQty = ExcelHelper.getCellValueAmoeba(row, 13).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    unit = ExcelHelper.getCellValueAmoeba(row, 14),
                    poNumber = poNo,
                    invoiceNumber = invoice,
                    detail = ExcelHelper.getCellValueAmoeba(row, 28),
                    itemType = if (itemType == "") "ZLAG" else itemType,
                    lotNo = lotNo.second,
                    unitPrice = ExcelHelper.getCellValueAmoeba(row, 15).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    costCenter = ExcelHelper.getCellValueAmoeba(row, 51),
                )
                if (lotNo.first == "Insert") {
                    purchaseOrderBacklogRepo.save(data)
                    count++
                }
                else if (lotNo.first == "Update") {
                    //lay id va cac truong khong co trong file tu ban ghi da ton tai, tranh update ra null
                    val existing = lotNo.third!!
                    data.id = existing.id
                    data.approved = existing.approved
                    data.synced = existing.synced
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

    fun generateLotNo(seqNo: BigDecimal, invoice: String, poNo: String, productionGroup: String): Triple<String, String, PurchaseOrderBacklog?> {
        val currentDate = OffsetDateTime.now().toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyMMdd"))
        val latestLot = purchaseOrderBacklogRepo.findLatestLotOfDay(currentDate, productionGroup)
        val replacedLot = purchaseOrderBacklogRepo.findLotOfPoInvoiceOnDay(seqNo, poNo, invoice, currentDate)
        if (replacedLot != null && replacedLot.approved == true) throw BusinessException("PO $poNo, Invoice $invoice đã được duyệt, không thể cập nhật")

        if (latestLot == null) //neu la lot dau cua ngay cua 1 bo phan thi mac dinh la 000
             return Triple("Insert", "$productionGroup${currentDate}000", null)
        else if (replacedLot == null) //neu khong phai lot dau cua ngay va PO-Invoice do chua ton tai thi phai tang seq
            {
                val nextSequence = latestLot?.lotNo!!.takeLast(3).toInt() + 1
                return Triple("Insert", "$productionGroup$currentDate${nextSequence.toString().padStart(3, '0')}", null)
            }
        else return Triple("Update", replacedLot.lotNo.toString(), replacedLot) //neu PO-Invoice da ton tai trong ngay do thi tra ve lot da ton tai
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

    fun getListForDropDown(status: String, isIncludeGe7Days: Boolean): BaseResponse<List<DropdownResponse>> {
        val listOrder = purchaseOrderBacklogRepo.getListForDropDown(status, isIncludeGe7Days)
        val dropDownList= listOrder.map { order ->
            DropdownResponse(
                "${order.invoiceNumber} ~ ${
                    order.createdDate?.toLocalDate()}",
                "${order.invoiceNumber} ~ ${
                    order.createdDate?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            )
        }.toMutableList().distinct()
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
            poNumber = it.poNumber.orEmpty(),
            invoiceNumber = it.invoiceNumber.orEmpty(),
            detail = it.detail.orEmpty(),
            itemType = it.itemType.orEmpty(),
            lotNo = it.lotNo.orEmpty(),
        ) }
    }
}