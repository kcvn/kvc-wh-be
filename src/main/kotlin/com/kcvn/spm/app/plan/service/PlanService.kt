package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.*
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
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
class PlanService(
    private val planRep: PlanRepository,
    private val planProductRep: PlanProductRepository,
    private val planProcessRep: PlanProcessRepository,
    private val planDetailRep: PlanDetailRepository,
    private val workResultRep: WorkResultRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository
) {
    fun getListPlan(request: PlanSearchRequest, pageable: Pageable): BasePagingResponse<ProductPlanModel> {
        val data = planProductRep.getListPlanProduct(request, pageable)
        val plans = data.first.map { x ->
            ProductPlanModel(
                id = x.id,
                productName = x.productName,
                frame_1 = x.frame_1,
                mold = x.mold,
                pcsSh = x.pcsSh,
                blockSh = x.blockSh
            )
        }
        return BasePagingResponse(plans, data.second)
    }

    fun getPlanDetail(request: PlanDetailRequest): ProductPlanDetailResponse {
        val response = ProductPlanDetailResponse()
        if (request.planProductId.isEmpty()) throw BusinessException(CommonUtils.getMessage("plan.invalidParam"))

        var colStartDate = OffsetDateTime.now()
        var colEndDate = OffsetDateTime.now()
        if (request.filterType == OrderFilterType.DATE) {
            if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))
            colStartDate = request.startDate
            colEndDate = request.endDate
        }
        if (request.filterType == OrderFilterType.ORDER) {
            val plan = planRep.getPlanByOrderCode(request.orderCode)
                ?: throw BusinessException(CommonUtils.getMessage("plan.notExistInOrder"))
            colStartDate = plan.startDate
            colEndDate = plan.endDate
        }

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)

        val planProduct = planProductRep.getById(request.planProductId)
            ?: throw BusinessException(CommonUtils.getMessage("data.notExist"))

        val planProcesses = planProcessRep.getListPlanProcess(request.planProductId)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)

        val workResults = workResultRep.getForPlan(colStartDate, colEndDate, listOf(planProduct.productName ?: ""))

        response.data = parentPlanProcess.map { x ->
            val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
            val planDetail = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_KEY }.map { t ->
                KeyValueResponse(
                    DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                    if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                )
            }
            val planAccumulations = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_ACCUMULATION_KEY }.map { t ->
                KeyValueResponse(
                    DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                    if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                )
            }

            val workResultData = workResults.filter { m -> m.processCode == x.processCode && m.layerCode == x.layerCode }.map { m ->
                KeyValueResponse(
                    DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd),
                    if (x.unit == ProcessUnit.BLOCK) m.goodTapeQuantity?.toString() else m.goodSheetQuantity?.toString()
                )
            }.sortedBy { m -> m.key }
            val workResultAccumulations = calculateAccumulation(workResultData)

            val planData = mutableListOf<PlanDataByProcessModel>()


            val productPlan = ProductPlanDetailModel(
                layerCode = x.layerCode,
                processCode = x.processCode,
                processName = x.processName,
                completionRate = x.completionRate,
                processConvertCode = x.processConvertCode,
                inventory = x.inventory
            )
            productPlan.processChildren = childrenPlanProcess.filter { m -> m.parentId == x.id }.map { m ->
                ProcessChildrenModel(
                    layerCode = m.layerCode,
                    processCode = m.processCode,
                    processName = m.processName,
                    inventory = m.inventory
                )
            }
            productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 }
                ?: 0) + (productPlan.inventory ?: 0)

            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, quantityByCalendars = planDetail))
            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, quantityByCalendars = planAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, quantityByCalendars = workResultData))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, quantityByCalendars = workResultAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

            productPlan.planData = planData
            productPlan
        }
        return response
    }

    fun exportExcel(request: PlanSearchRequest): FileContentModel {
        var colStartDate = OffsetDateTime.now()
        var colEndDate = OffsetDateTime.now()
        if (request.filterType == OrderFilterType.DATE) {
            if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))
            colStartDate = request.startDate
            colEndDate = request.endDate
        }
        if (request.filterType == OrderFilterType.ORDER) {
            val plan = planRep.getPlanByOrderCode(request.orderCode ?: "")
                ?: throw BusinessException(CommonUtils.getMessage("plan.notExistInOrder"))
            colStartDate = plan.startDate
            colEndDate = plan.endDate
        }

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        val columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)

        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcel(planProducts, colStartDate, colEndDate)
        if (dataExports.isEmpty()) throw BusinessException(CommonUtils.getMessage("plan.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportPlanTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val headerRow = sheet.getRow(0)
        var headerCol = 8
        val headerStyle = headerRow.getCell(0).cellStyle
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        val style = ExcelHelper.getCellStyleCommon(workbook)
        var rowNumber = 1
        for (planProduct in planProducts) {
            val dataExport = dataExports.find { x -> x.id == planProduct.id }
            if (dataExport == null || dataExport.productPlanDetails.isNullOrEmpty()) continue

            rowNumber = generateExcelRowPlanProduct(workbook, sheet, rowNumber, style, columns, planProduct)

            for (planProcess in dataExport.productPlanDetails!!) {
                val rowIndex1 = generateExcelRowPlanProcess(workbook, sheet, rowNumber, style, planProcess)
                val rowIndex2 = generateExcelRowPlanData(workbook, sheet, rowNumber, style, columns, planProcess)
                rowNumber = rowIndex1
                if (rowIndex1 > rowIndex2) {
                    for (rowIndex in rowIndex2 until rowIndex1) {
                        val dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                        ExcelHelper.setCellValue(workbook, dataRow, 7, style, "")
                        var colIndex = 8
                        for (col in columns) {
                            ExcelHelper.setCellValue(workbook, dataRow, colIndex, style, "")
                            colIndex++
                        }
                    }
                }
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportPlan", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }

    private fun generateExcelRowPlanProduct(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        columns: List<CalendarResponse>,
        data: PlanProduct
    ): Int {
        var rowIndex = rowNumber
        var dataRow = sheet.createRow(rowIndex)
        val indexColor = IndexedColors.CORNFLOWER_BLUE.index
        ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, CommonUtils.getMessage("excel.colProductName"), true, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, CommonUtils.getMessage("excel.colFrame"), false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, CommonUtils.getMessage("excel.colMold"), false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, CommonUtils.getMessage("excel.colPcsSh"), false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, CommonUtils.getMessage("excel.colBlockSh"), false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, false, true, false, true, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 7, style, "", false, true, true, false, true, true, indexColor)
        var colIndex = 8
        for (col in columns) {
            ExcelHelper.setCellValueCustom(workbook, dataRow, colIndex, style, "", true, true, true, true, true, true, indexColor)
            colIndex++
        }

        rowIndex++
        dataRow = sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, data.productName, true, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, data.frame_1, false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, data.mold, false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, data.pcsSh?.toString(), false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, data.blockSh?.toString(), false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, false, false, true, false, true, indexColor)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 7, style, "", false, true, false, true, false, true, indexColor)
        colIndex = 8
        for (col in columns) {
            ExcelHelper.setCellValueCustom(workbook, dataRow, colIndex, style, "", true, true, true, true, false, true, indexColor)
            colIndex++
        }
        rowIndex++
        return rowIndex
    }

    private fun generateExcelRowPlanProcess(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: ProductPlanDetailModel
    ): Int {
        var rowIndex = rowNumber
        var dataRow = sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, data.layerCode, true, false, true, false, false, true)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, data.processCode, false, false, true, false, false, true)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, data.processName, false, false, true, false, false, false)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, data.completionRate?.toString(), false, false, true, false, false, true)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, data.inventory?.toString(), false, false, true, false, false, true)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, data.sumInventory?.toString(), false, false, true, false, false, true)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, data.processConvertCode, false, true, true, false, false, true)

        rowIndex++
        var count = 0
        if ((data.processChildren?.size ?: 0) > ((data.planData?.size ?: 0) - 1)) {
            for (process in data.processChildren!!) {
                dataRow = sheet.createRow(rowIndex)
                if (count < data.processChildren!!.size - 1) {
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, process.layerCode, true, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, process.processCode, false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, process.processName, false, false, false, false, false, false)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, "", false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, process.inventory?.toString(), false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, true, false, false, false, true)
                } else {
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, process.layerCode, true, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, process.processCode, false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, process.processName, false, false, false, true, false, false)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, "", false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, process.inventory?.toString(), false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, true, false, true, false, true)
                }
                rowIndex++
                count++
            }
        } else {
            for (i in rowIndex until (rowIndex + 4)) {
                val process = if (data.processChildren.isNullOrEmpty()) null
                    else if (count < data.processChildren!!.size) data.processChildren!![count] else null
                dataRow = sheet.createRow(rowIndex)
                if (count < 3) {
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, process?.layerCode, true, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, process?.processCode, false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, process?.processName, false, false, false, false, false, false)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, "", false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, process?.inventory?.toString(), false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, false, false, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, true, false, false, false, true)
                } else {
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 0, style, process?.layerCode, true, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, process?.processCode, false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 2, style, process?.processName, false, false, false, true, false, false)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 3, style, "", false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, process?.inventory?.toString(), false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 5, style, "", false, false, false, true, false, true)
                    ExcelHelper.setCellValueCustom(workbook, dataRow, 6, style, "", false, true, false, true, false, true)
                }
                rowIndex++
                count++
            }
        }
        return rowIndex
    }

    private fun generateExcelRowPlanData(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        columns: List<CalendarResponse>,
        data: ProductPlanDetailModel
    ): Int {
        var rowIndex = rowNumber
        for (planData in data.planData!!) {
            val dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            ExcelHelper.setCellValue(workbook, dataRow, 7, style, planData.title)
            var colIndex = 8
            for (col in columns) {
                val value = planData.quantityByCalendars?.find { x -> x.key == col.key }?.value
                ExcelHelper.setCellValueWithCalendar(workbook, dataRow, colIndex, style, value, col.isHoliday)
                colIndex++
            }
            rowIndex++
        }

        return rowIndex
    }

    private fun getDataExportExcel(planProducts: List<PlanProduct>, colStartDate: OffsetDateTime, colEndDate: OffsetDateTime): List<PlanExportExcelModel> {
        val planProductIds = planProducts.mapNotNull { x -> x.id }
        val productNames = planProducts.mapNotNull { x -> x.productName }.distinct()

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }.sortedBy { x -> x.planProductId }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)

        val workResults = workResultRep.getForPlan(colStartDate, colEndDate, productNames)

        val data = mutableListOf<PlanExportExcelModel>()

        for (planProduct in planProducts) {
            val productExport = PlanExportExcelModel(
                id = planProduct.id,
                productName = planProduct.productName,
                frame_1 = planProduct.frame_1,
                mold = planProduct.mold,
                pcsSh = planProduct.pcsSh,
                blockSh = planProduct.blockSh
            )
            productExport.productPlanDetails = parentPlanProcess.filter { x -> x.planProductId == planProduct.id }.map { x ->
                val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
                val planDetail = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_KEY }.map { t ->
                    KeyValueResponse(
                        DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                    )
                }
                val planAccumulations = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_ACCUMULATION_KEY }.map { t ->
                    KeyValueResponse(
                        DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                    )
                }

                val workResultData = workResults.filter { m ->
                    m.itemName == planProduct.productName
                        && m.processCode == x.processCode
                        && m.layerCode == x.layerCode
                }.map { m ->
                    KeyValueResponse(
                        DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) m.goodTapeQuantity?.toString() else m.goodSheetQuantity?.toString()
                    )
                }.sortedBy { m -> m.key }
                val workResultAccumulations = calculateAccumulation(workResultData)

                val planData = mutableListOf<PlanDataByProcessModel>()

                val productPlan = ProductPlanDetailModel(
                    layerCode = x.layerCode,
                    processCode = x.processCode,
                    processName = x.processName,
                    completionRate = x.completionRate,
                    processConvertCode = x.processConvertCode,
                    inventory = x.inventory
                )
                productPlan.processChildren = childrenPlanProcess.filter { m -> m.parentId == x.id }.map { m ->
                    ProcessChildrenModel(
                        layerCode = m.layerCode,
                        processCode = m.processCode,
                        processName = m.processName,
                        inventory = m.inventory
                    )
                }
                productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 }
                    ?: 0) + (productPlan.inventory ?: 0)

                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, quantityByCalendars = planDetail))
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, quantityByCalendars = planAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, quantityByCalendars = workResultData))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, quantityByCalendars = workResultAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

                productPlan.planData = planData
                productPlan
            }
            data.add(productExport)
        }


        return data
    }

    private fun calculateAccumulation(data: List<KeyValueResponse>, firstValue: Int? = null): List<KeyValueResponse> {
        var value = firstValue ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for (item in data) {
            value += (item.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, value.toString()))
        }
        return response
    }

    private fun calculateDifference(sourceData: List<KeyValueResponse>, compareData: List<KeyValueResponse>): List<KeyValueResponse> {
        val response = mutableListOf<KeyValueResponse>()
        for (item in sourceData) {
            val compareValue = compareData.find { x -> x.key == item.key }
            val diffValue = (compareValue?.value?.toInt() ?: 0) - (item.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, diffValue.toString()))
        }
        return response
    }

}