package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.*
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PlanSummaryResponse
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
    //region PLAN
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
                frame_1 = planProduct.frame_1,
                mold = planProduct.mold,
                layerCode = x.layerCode,
                processCode = x.processCode,
                processName = x.processName,
                processNameJp = x.processNameJp,
                completionRate = x.completionRate,
                processConvertCode = x.processConvertCode,
                processSequence = x.processSequence,
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
            productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 } ?: 0) + (productPlan.inventory ?: 0)

            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))
            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, titleKey = PlanTitle.PLAN_ACCUMULATION_KEY, quantityByCalendars = planAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY, quantityByCalendars = workResultAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, titleKey = PlanTitle.DIFFERENCE_KEY, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

            productPlan.planData = planData
            productPlan
        }.sortedBy { x -> x.processSequence }
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
        if (dataExports.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))

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
                            ExcelHelper.setCellValueWithCalendar(workbook, dataRow, colIndex, style, "", col.isHoliday)
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
        val indexColor = IndexedColors.PALE_BLUE.index
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = CommonUtils.getMessage("excel.colProductName"),
            isBorderLeft = true, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = CommonUtils.getMessage("excel.colFrame"),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = CommonUtils.getMessage("excel.colMold"),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = CommonUtils.getMessage("excel.colPcsSh"),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = CommonUtils.getMessage("excel.colBlockSh"),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 7, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
            isBold = true, isAlignCenter = true, indexColor = indexColor
        )
        var colIndex = 8
        for (col in columns) {
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = colIndex, styleTemplate = style, value = "",
                isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = true,
                isBold = true, isAlignCenter = true, indexColor = indexColor)
            colIndex++
        }

        rowIndex++
        dataRow = sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.productName,
            isBorderLeft = true, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = data.frame_1,
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = data.mold,
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = data.pcsSh?.toString(),
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = data.blockSh?.toString(),
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 7, styleTemplate = style, value = "",
            isBorderLeft = false, isBorderRight = true, isBorderTop = false, isBorderBottom = true,
            isBold = false, isAlignCenter = true, indexColor = indexColor
        )
        colIndex = 8
        for (col in columns) {
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = colIndex, styleTemplate = style, value = "",
                isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = true,
                isBold = false, isAlignCenter = true, indexColor = indexColor
            )
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
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.layerCode,
            isBorderLeft = true, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = data.processCode,
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = data.processName,
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = data.completionRate?.toString(),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = data.inventory?.toString(),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = data.sumInventory?.toString(),
            isBorderLeft = false, isBorderRight = false, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = data.processConvertCode,
            isBorderLeft = false, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = true
        )

        rowIndex++
        var count = 0
        if ((data.processChildren?.size ?: 0) > ((data.planData?.size ?: 0) - 1)) {
            for (process in data.processChildren!!) {
                dataRow = sheet.createRow(rowIndex)
                if (count < data.processChildren!!.size - 1) {
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = process.layerCode,
                        isBorderLeft = true, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = process.processCode,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = process.processName,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = false
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = process.inventory?.toString(),
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                } else {
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = process.layerCode,
                        isBorderLeft = true, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = process.processCode,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = process.processName,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = false
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = process.inventory?.toString(),
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = true, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
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
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = process?.layerCode,
                        isBorderLeft = true, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = process?.processCode,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = process?.processName,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = false
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = process?.inventory?.toString(),
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
                        isBold = false, isAlignCenter = true
                    )
                } else {
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = process?.layerCode,
                        isBorderLeft = true, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = process?.processCode,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 2, styleTemplate = style, value = process?.processName,
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = false
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 3, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 4, styleTemplate = style, value = process?.inventory?.toString(),
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 5, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = false, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 6, styleTemplate = style, value = "",
                        isBorderLeft = false, isBorderRight = true, isBorderTop = false, isBorderBottom = true,
                        isBold = false, isAlignCenter = true
                    )
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
                    frame_1 = planProduct.frame_1,
                    mold = planProduct.mold,
                    layerCode = x.layerCode,
                    processCode = x.processCode,
                    processName = x.processName,
                    processNameJp = x.processNameJp,
                    completionRate = x.completionRate,
                    processConvertCode = x.processConvertCode,
                    processSequence = x.processSequence,
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

                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, titleKey = PlanTitle.PLAN_ACCUMULATION_KEY, quantityByCalendars = planAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY, quantityByCalendars = workResultAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, titleKey = PlanTitle.DIFFERENCE_KEY, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

                productPlan.planData = planData
                productPlan
            }.sortedBy { x -> x.processSequence }
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

    //endregion

    //region PLAN SUMMARY

    fun getPlanSummary(request: PlanSearchRequest): PlanSummaryResponse {
        val response = PlanSummaryResponse()
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
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)

        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcel(planProducts, colStartDate, colEndDate)
        val dataExportFlattens = dataExports.asSequence().mapNotNull { x -> x.productPlanDetails }.flatten().filter {
            x -> !x.processConvertCode.isNullOrEmpty()
            && (PlanProcessSummary.DATA.any { m -> m == x.processConvertCode } || x.processConvertCode!!.startsWith(ProcessConvertCode.M))
        }

        val dataSummary = dataExportFlattens.groupBy { x -> x.processConvertCode }.map { x ->
            val process = x.value.first()
            val summary = PlanSummaryModel(
                processName = process.processName,
                processNameJp = process.processNameJp,
                processConvertCode = x.key,
                processSequence = process.processSequence,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = "",
                        planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                            .groupBy { m -> Pair(m.titleKey, m.title) }
                            .map { m ->
                                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }
                    )
                )
            )
            summary
        }.toMutableList()

        val dataDucLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH }
        if (dataDucLo.isNotEmpty()) {
            val moldByFrame1s = Mold.DATA_BY_FRAME1(request.frame_1)
            val ducLo = PlanSummaryModel(
                processName = CommonUtils.getMessage("excel.rowDucLo"),
                processNameJp = dataDucLo.first().processNameJp,
                processConvertCode = "${ProcessConvertCode.T}/${ProcessConvertCode.TH}",
                processSequence = dataDucLo.first().processSequence,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = if (moldByFrame1s.size > 1) CommonUtils.getMessage("excel.rowTotal") else "",
                        planSummaryData = dataDucLo.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )
            if (moldByFrame1s.size > 1) {
                for (mold in moldByFrame1s) {
                    var dataMold = dataExportFlattens.filter {
                        x -> x.mold == mold
                        && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH)
                    }.groupBy { x -> x.mold }.mapNotNull { x ->
                        PlanSummaryDetailModel(
                            type = x.key,
                            planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                    val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                    data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }
                        )
                    }.firstOrNull()
                    if (dataMold == null) {
                        dataMold = PlanSummaryDetailModel(
                            type = mold,
                            planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                        )
                    }
                    ducLo.details!!.add(dataMold)
                }
            }

            dataSummary.removeAll(dataDucLo)
            dataSummary.add(ducLo)
        }

        val dataInMach = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.TAN || x.processConvertCode == ProcessConvertCode.ZEN }
        if (dataInMach.isNotEmpty()) {
            val inMach = PlanSummaryModel(
                processName = CommonUtils.getMessage("excel.rowInMach"),
                processNameJp = dataInMach.first().processNameJp,
                processConvertCode = "${ProcessConvertCode.TAN}/${ProcessConvertCode.ZEN}",
                processSequence = dataInMach.first().processSequence,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
                        planSummaryData = dataInMach.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )

            for (code in listOf(ProcessConvertCode.TAN, ProcessConvertCode.ZEN)) {
                var dataTanZen = dataExportFlattens.filter { x -> x.processConvertCode == code }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                    PlanSummaryDetailModel(
                        type = x.key,
                        planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                            .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }
                    )
                }.firstOrNull()
                if (dataTanZen == null) {
                    dataTanZen = PlanSummaryDetailModel(
                        type = code,
                        planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                    )
                }
                inMach.details!!.add(dataTanZen)
            }

            dataSummary.removeAll(dataInMach)
            dataSummary.add(inMach)
        }

        val dataInLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.HP_ALL || x.processConvertCode == ProcessConvertCode.HP }
        if (dataInLo.isNotEmpty()) {
            val inLo = PlanSummaryModel(
                processName = CommonUtils.getMessage("excel.rowInLo"),
                processNameJp = dataInLo.first().processNameJp,
                processConvertCode = "${ProcessConvertCode.HP_ALL}/${ProcessConvertCode.HP}",
                processSequence = dataInLo.first().processSequence,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
                        planSummaryData = dataInLo.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )

            for (code in listOf(ProcessConvertCode.HP_ALL, ProcessConvertCode.HP)) {
                var dataHP = dataExportFlattens.filter { x -> x.processConvertCode == code }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                    PlanSummaryDetailModel(
                        type = x.key,
                        planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                            .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }
                    )
                }.firstOrNull()
                if (dataHP == null) {
                    dataHP = PlanSummaryDetailModel(
                        type = code,
                        planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                    )
                }
                inLo.details!!.add(dataHP)
            }

            dataSummary.removeAll(dataInLo)
            dataSummary.add(inLo)
        }

        val dataGhepLop = dataSummary.filter { x -> !x.processConvertCode.isNullOrEmpty() && x.processConvertCode!!.startsWith(ProcessConvertCode.M) }
        if (dataGhepLop.isNotEmpty()) {
            val ghepLop = PlanSummaryModel(
                processName = CommonUtils.getMessage("excel.rowGhepLop"),
                processNameJp = dataGhepLop.first().processNameJp,
                processConvertCode = "${ProcessConvertCode.M_ALL}/${ProcessConvertCode.M_ANY}",
                processSequence = dataGhepLop.first().processSequence,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
                        planSummaryData = dataGhepLop.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )

            var mAll = dataExportFlattens.filter { x -> x.processConvertCode == ProcessConvertCode.M_ALL }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                PlanSummaryDetailModel(
                    type = x.key,
                    planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                        .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                            val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                            data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                            data
                        }
                )
            }.firstOrNull()
            if (mAll == null) {
                mAll = PlanSummaryDetailModel(
                    type = ProcessConvertCode.M_ALL,
                    planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                )
            }
            ghepLop.details!!.add(mAll)

            var mTan = dataExportFlattens.filter {
                x -> !x.processConvertCode.isNullOrEmpty()
                && x.processConvertCode != ProcessConvertCode.M_ALL
                && x.processConvertCode!!.startsWith(ProcessConvertCode.M)
            }.map { x ->
                x.processConvertCode = ProcessConvertCode.M_ANY
                x
            }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                PlanSummaryDetailModel(
                    type = x.key,
                    planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                        .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                            val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                            data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                            data
                        }
                )
            }.firstOrNull()
            if (mTan == null) {
                mTan = PlanSummaryDetailModel(
                    type = ProcessConvertCode.M_ANY,
                    planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                )
            }
            ghepLop.details!!.add(mTan)

            dataSummary.removeAll(dataGhepLop)
            dataSummary.add(ghepLop)
        }

        response.data = dataSummary.sortedBy { x -> x.processSequence }

        return response

    }

    fun exportExcelSummary(request: PlanSearchRequest): FileContentModel {
        val dataSummary = getPlanSummary(request)
        if (dataSummary.data.isNullOrEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportPlanSummaryTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val headerRow = sheet.getRow(0)
        var headerCol = 3
        val headerStyle = headerRow.getCell(0).cellStyle
        for (col in dataSummary.columns!!) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        val style = ExcelHelper.getCellStyleCommon(workbook)
        var rowNumber = 1
        for (data in dataSummary.data!!) {
            generateExcelColProcessInPlanSummary(workbook, sheet, rowNumber, style, data)

            var rowIndex = rowNumber
            for (detail in data.details!!) {
                var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                ExcelHelper.setCellValueCustom(
                    workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = detail.type,
                    isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
                    isBold = false, isAlignCenter = false
                )
                rowIndex++

                for (i in 1 until 5) {
                    dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                    ExcelHelper.setCellValueCustom(
                        workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = "",
                        isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = (i == 4),
                        isBold = false, isAlignCenter = false
                    )
                    rowIndex++
                }

                rowIndex = rowNumber
                for (item in detail.planSummaryData!!) {
                    dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                    ExcelHelper.setCellValue(workbook, dataRow, 2, style, item.title)

                    var colIndex = 3
                    for (col in dataSummary.columns!!) {
                        val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                        ExcelHelper.setCellValueWithCalendar(workbook, dataRow, colIndex, style, value, col.isHoliday)
                        colIndex++
                    }
                    rowIndex++
                }
                rowNumber = rowIndex
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportPlanSummary", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }

    private fun generateExcelColProcessInPlanSummary(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: PlanSummaryModel
    ): Int {
        var rowIndex = rowNumber
        var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processName,
            isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processNameJp,
            isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processConvertCode,
            isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )

        rowIndex++
        for (i in 3 until (data.details!!.size * 5)) {
            dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = "",
                isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = (i == ((data.details!!.size * 5) - 1)),
                isBold = false, isAlignCenter = false
            )
            rowIndex++
        }
        return rowIndex
    }

    //endregion

}