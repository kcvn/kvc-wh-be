package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanValidateModel
import com.kcvn.spm.app.plan.payload.request.CreatePlanRequest
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.Frame1
import com.kcvn.spm.common.constants.ProcessConvertCode
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.repository.EquipmentProductivityRepository
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class CreatePlanService(
    private val orderInfoRep: OrderInfoRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val productProcessRep: ProductProcessRepository,
    private val productRep: ProductRepository,
    private val completionRateProcessProductRep: CompletionRateProcessProductRepository,
    private val equipmentProductivityRep: EquipmentProductivityRepository
) {

    fun checkInventory(request: CreatePlanRequest): BaseResponse<Boolean> {
        if (request.startDate == null) throw BusinessException("Chưa có thông tin ngày bắt đầu và ngày kết thúc kế hoạch")
        val inventory = inventoryProductRep.findDateInventoryProduct(request.startDate!!)
        return BaseResponse(inventory != null)
    }

    fun validate(request: CreatePlanRequest): BaseResponse<FileContentModel> {
        if (request.startDate == null || request.endDate == null)
            throw BusinessException("Chưa có thông tin ngày bắt đầu và ngày kết thúc kế hoạch")

        val startDate = request.startDate!!
        val endDate = request.endDate!!

        val orderInfo = orderInfoRep.getOrderInfoByTimeRange(startDate, endDate)
        if (orderInfo.isEmpty())
            throw BusinessException("Không có dữ liệu xuất hàng " +
                "từ ngày ${DateTimeHelper.toString(startDate, DateTimeFormat.dd_MM_yyyy)} " +
                "đến ngày ${DateTimeHelper.toString(endDate, DateTimeFormat.dd_MM_yyyy)}"
            )

        val lstDate = DateTimeHelper.toCalendarColumn(startDate, endDate)

        val productNames = orderInfo.mapNotNull { it.productName }.sortedBy { it }.distinct()
        val productShortcutNames = productNames.map { it.substring(it.length - 7, it.length) }
        val productInfo = productRep.getByName(productNames)
        val productProcesses = productProcessRep.getByProductName(productNames).filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
        }
        val completionRateInfo = completionRateProcessProductRep.getByProductName(productShortcutNames)

        val processGroupCodes = productProcesses.mapNotNull { x -> x.processGroup }
        val equipmentInfo = equipmentProductivityRep.getByProcessGroup(processGroupCodes)
        val processStatisticCodeChecks = listOf(ProcessStatisticCode.T, ProcessStatisticCode.GHEPLOP_GIAAPNHIET, ProcessStatisticCode.SNAP)

        val planValidates = mutableListOf<PlanValidateModel>()
        for (item in productNames) {
            val errors = mutableListOf<String>()
            val product = productInfo.firstOrNull { x -> x.name == item }
            if (product == null) errors.add("Không tồn tại thông tin sản phẩm")

            val productProcess = productProcesses.filter { x -> x.productName == item && x.processCode?.toIntOrNull() != 0 }
            if (productProcess.isEmpty()) {
                errors.add("Chưa có thông tin công đoạn")
            } else {
                if (productProcess.any { x -> x.processConvertCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Mã chuyển đổi")
                if (productProcess.any { x -> x.processStatisticCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Mã thống kê")
                if (productProcess.any { x -> x.processInventoryCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Mã gộp tồn kho")
                if (productProcess.any { x -> x.dayOfImplementation.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Ngày thứ thực hiện")
            }

            for (date in lstDate) {
                val iDate = date.key!!.toInt()
                val completionRate = completionRateInfo.find { x ->
                    x.productNameShortcut == item.substring(item.length - 7, item.length)
                        && x.effectiveDate != null
                        && iDate >= DateTimeHelper.toString(x.effectiveDate!!, DateTimeFormat.yyyyMMdd).toInt()
                        && (x.expirationDate == null || iDate <= DateTimeHelper.toString(x.expirationDate!!, DateTimeFormat.yyyyMMdd).toInt())
                }
                if (completionRate == null) {
                    errors.add("Chưa có thông tin tỷ lệ đạt hợp lệ tại ${date.value}")
                    break
                }
            }

            val equipmentByProducts = equipmentInfo.filter { x -> x.frame_1 == product!!.frame_1 }
            if (equipmentByProducts.isEmpty()) {
                errors.add("Chưa có cấu hình năng suất máy cho line ${product!!.frame_1}")
            } else {
                for (process in productProcess) {
                    var eqConfigs = equipmentByProducts.filter { x -> x.grpProcess == process.processGroup }
                    if (eqConfigs.isEmpty()) {
                        errors.add("Chưa có cấu hình năng suất máy cho nhóm công đoạn ${process.processGroup}")
                    } else {
                        if (product!!.frame_1 != Frame1.MU) continue

                        if (
                            processStatisticCodeChecks.contains(process.processStatisticCode)
                            || process.processConvertCode == ProcessConvertCode.DAN_2L
                            || process.processCode == "214220"
                        ) {
                            eqConfigs = eqConfigs.filter { x -> x.mold == product.mold }
                            if (eqConfigs.isEmpty()) {
                                errors.add("Chưa có cấu hình năng suất máy cho khuôn đục ${product.mold} của nhóm công đoạn ${process.processGroup}")
                            }
                        }

                    }
                }
            }

            if (errors.isNotEmpty()) planValidates.add(genPlanValidModel(item, errors))
        }

        val fileContent = exportFilePlanValidate(planValidates)

        return BaseResponse(fileContent)
    }

    private fun exportFilePlanValidate(planValidates: List<PlanValidateModel>): FileContentModel {
        if (planValidates.isEmpty()) return FileContentModel()

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/PlanValidateTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val style = ExcelHelper.getCellStyleCommon(workbook)
        var rowNumber = 1

        for (item in planValidates) {
            val dataRow = sheet.createRow(rowNumber)

            ExcelHelper.setCellValue(dataRow, 0, style, item.productName)
            ExcelHelper.setCellValue(dataRow, 1, style, item.message)

            rowNumber++
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportPlanValidate", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return response
    }
    private fun genPlanValidModel(productName: String, errors: List<String>, date: OffsetDateTime? = null): PlanValidateModel {
        return PlanValidateModel(
            productName = productName,
            date = if (date != null) DateTimeHelper.toString(date, DateTimeFormat.dd_MM_yyyy) else null,
            message = errors.joinToString(separator = "; ")
        )
    }

}