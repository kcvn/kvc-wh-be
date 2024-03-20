package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.*
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.app.plan.payload.response.PlanSummaryResponse
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper.Companion.truncateDecimal
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class EquipmentProductivityService(
    private val equipmentProductivityRepository: EquipmentProductivityRepository,
    private val planRepository: PlanRepository,
    private val planProductRep: PlanProductRepository,
    private val planProcessRep: PlanProcessRepository,
    private val planDetailRep: PlanDetailRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val commonCategoryRep: CommonCategoryRepository
) {

    fun getPaginatedEquipmentProductivityPlan(
        request: PlanSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): PagingEquipmentProdResponse? {

        val planSummary =getPlanSummary(request)
        val equipmentMachine = equipmentProductivityRepository.getEquipmentProductivity()

        val result = PagingEquipmentProdResponse()
        result.columns = planSummary.columns
        val equipmentProductivityList = mutableListOf<EquipmentProductivityModel>()
        planSummary.data?.let { data ->
            for (planSummaryModel in data) {
                val equipmentProductivityModel = EquipmentProductivityModel(
                    frame1 = planSummaryModel.frame1,
                    processName = planSummaryModel.processName,
                    processNameJp = planSummaryModel.processNameJp,
                    processConvertCode = planSummaryModel.processConvertCode,
                    processCode = planSummaryModel.processCode,
                    unit = planSummaryModel.unit,
                    processDetail = mutableListOf()
                )

                planSummaryModel.details?.let { details ->
                    for (processSummaryDetailModel in details) {
                        var equipmentMachineValue: BigDecimal?
                        val equipmentMachineModel = if (equipmentProductivityModel.processName == CommonUtils.getMessage("excel.rowDucLo")) {
                            equipmentMachine.firstOrNull { it.processCode == planSummaryModel.processCode && it.frame_1 == planSummaryModel.frame1 && it.mold == processSummaryDetailModel.type }
                        } else {
                            equipmentMachine.firstOrNull { it.processCode == planSummaryModel.processCode && it.frame_1 == planSummaryModel.frame1 }
                        }
                        equipmentMachineValue = when {
                            equipmentMachineModel != null -> {
                                when (equipmentProductivityModel.unit) {
                                    ProcessUnit.SHEET -> equipmentMachineModel.sheetDay
                                    ProcessUnit.SET -> equipmentMachineModel.setDay
                                    else -> equipmentMachineModel.blockDay
                                }
                            }
                            else -> null
                        }
                        val processDetailModel = ProcessDetailModel(
                            name = processSummaryDetailModel.type,
                            totalProcess = null,
                            processDetailList = mutableListOf()
                        )
                        processSummaryDetailModel.planSummaryData?.let { planData ->
                            for (planDataByProcessModel in planData) {
                                //process
                                val processDetailListModel = ProcessDetailListModel(
                                    type = planDataByProcessModel.title ?: "",
                                    quantityByCalendars = planDataByProcessModel.quantityByCalendars?.toMutableList() ?: mutableListOf()
                                )
                                processDetailModel.processDetailList.add(processDetailListModel)
                                //calculate total
                                if (planDataByProcessModel.title == ProcessPlan.PROCESS) {
                                    processDetailModel.totalProcess = processDetailListModel.quantityByCalendars
                                        .mapNotNull { it.value?.toDoubleOrNull() }
                                        .sum()
                                        .toInt()
                                }
                                //machine
                                val machineDetailListModel = ProcessDetailListModel(
                                    type = ProcessPlan.MACHINE,
                                    quantityByCalendars = result.columns?.map { column ->
                                        KeyValueResponse(
                                            key = column.key,
                                            value = equipmentMachineValue?.toInt()?.toString() ?: ""
                                        )
                                    }?.toMutableList() ?: mutableListOf()
                                )
                                processDetailModel.processDetailList.add(machineDetailListModel)

                                //machineRate
                                val machineNumberDetailListModel = ProcessDetailListModel(
                                    type = ProcessPlan.MACHINENUMBER,
                                    quantityByCalendars = planDataByProcessModel.quantityByCalendars?.map { column ->
                                        val value = column.value?.toDoubleOrNull() ?: 0.0
                                        val equipmentMachineValueDouble = equipmentMachineValue?.toDouble() ?: 0.0
                                        val newValue = if (equipmentMachineValueDouble != 0.0) {
                                            val formattedValue = "%.1f".format(value / equipmentMachineValueDouble)
                                            formattedValue
                                        } else {
                                            ""
                                        }
                                        KeyValueResponse(
                                            key = column.key,
                                            value = newValue
                                        )
                                    }?.toMutableList() ?: mutableListOf()
                                )
                                processDetailModel.processDetailList.add(machineNumberDetailListModel)


                            }
                        }
                        equipmentProductivityModel.processDetail?.add(processDetailModel)
                    }
                }
                equipmentProductivityList.add(equipmentProductivityModel)
            }
        }

        result.data = equipmentProductivityList
        return result

    }




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
            val plan = planRepository.getPlanByOrderCode(request.orderCode ?: "")
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

        val types = listOf(MasterDataType.KHUNG_1)
        val dataFrame = commonCategoryRep.getByType(types)
        val listFrame1 = dataFrame.map { x-> x.value }

        val dataSummary = dataExportFlattens.groupBy { x -> Pair(x.processConvertCode, x.frame_1) }.map { x ->
            val process = x.value.first()
            val summary = PlanSummaryModel(
                processName = process.processName,
                processNameJp = process.processNameJp,
                processConvertCode = x.key.first,
                processSequence = process.processSequence,
                frame1 = process.frame_1,
                processCode = process.processCode,
                unit = process.unit,
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


        for(frame1 in listFrame1){
            val dataDucLo = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH) }
            if (dataDucLo.isNotEmpty()) {
                val moldByFrame1s = Mold.DATA_BY_FRAME1(request.frame_1)
                val ducLo = PlanSummaryModel(
                    processName = CommonUtils.getMessage("excel.rowDucLo"),
                    processNameJp = dataDucLo.first().processNameJp,
                    processConvertCode = "${ProcessConvertCode.T}/${ProcessConvertCode.TH}",
                    processSequence = dataDucLo.first().processSequence,
                    details = mutableListOf(),
                    frame1 = dataDucLo.first().frame1,
                    processCode = dataDucLo.first().processCode,
                    unit =dataDucLo.first().unit
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
                                planSummaryData = mutableListOf(PlanDataByProcessModel(title = ProcessPlan.PROCESS, titleKey = ProcessPlan.PROCESS, quantityByCalendars = listOf()))
                            )
                        }
                        ducLo.details!!.add(dataMold)
                    }
                }

                dataSummary.removeAll(dataDucLo)
                dataSummary.add(ducLo)
            }

            val dataInMach = dataSummary.filter { x -> x.frame1 == frame1 &&(x.processConvertCode == ProcessConvertCode.TAN || x.processConvertCode == ProcessConvertCode.ZEN)  }
            if (dataInMach.isNotEmpty()) {
                val inMach = PlanSummaryModel(
                    processName = CommonUtils.getMessage("excel.rowInMach"),
                    processNameJp = dataInMach.first().processNameJp,
                    processConvertCode = "${ProcessConvertCode.TAN}/${ProcessConvertCode.ZEN}",
                    processSequence = dataInMach.first().processSequence,
                    frame1 = dataInMach.first().frame1,
                    processCode = dataInMach.first().processCode,
                    unit =dataInMach.first().unit,
                    details = mutableListOf(
                        PlanSummaryDetailModel(
                            type = CommonUtils.getMessage(""),
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

                dataSummary.removeAll(dataInMach)
                dataSummary.add(inMach)
            }

            val dataInLo = dataSummary.filter { x -> x.frame1 == frame1 &&(x.processConvertCode == ProcessConvertCode.HP_ALL || x.processConvertCode == ProcessConvertCode.HP) }
            if (dataInLo.isNotEmpty()) {
                val inLo = PlanSummaryModel(
                    processName = CommonUtils.getMessage("excel.rowInLo"),
                    processNameJp = dataInLo.first().processNameJp,
                    processConvertCode = "${ProcessConvertCode.HP_ALL}/${ProcessConvertCode.HP}",
                    processSequence = dataInLo.first().processSequence,
                    frame1 = dataInLo.first().frame1,
                    processCode = dataInLo.first().processCode,
                    unit =dataInLo.first().unit,
                    details = mutableListOf(
                        PlanSummaryDetailModel(
                            type = CommonUtils.getMessage(""),
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
                dataSummary.removeAll(dataInLo)
                dataSummary.add(inLo)
            }

            val dataGhepLop = dataSummary.filter { x -> x.frame1 == frame1 &&(!x.processConvertCode.isNullOrEmpty() && x.processConvertCode!!.startsWith(ProcessConvertCode.M)) }
            if (dataGhepLop.isNotEmpty()) {
                val ghepLop = PlanSummaryModel(
                    processName = CommonUtils.getMessage("excel.rowGhepLop"),
                    processNameJp = dataGhepLop.first().processNameJp,
                    processConvertCode = "${ProcessConvertCode.M_ALL}/${ProcessConvertCode.M_ANY}",
                    processSequence = dataGhepLop.first().processSequence,
                    frame1 = dataGhepLop.first().frame1,
                    processCode = dataGhepLop.first().processCode,
                    unit =dataGhepLop.first().unit,
                    details = mutableListOf(
                        PlanSummaryDetailModel(
                            type = CommonUtils.getMessage(""),
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
                dataSummary.removeAll(dataGhepLop)
                dataSummary.add(ghepLop)
            }
        }

        response.data = dataSummary.sortedBy { x -> x.processSequence }
        return response

    }
    private fun getDataExportExcel(planProducts: List<PlanProduct>, colStartDate: OffsetDateTime, colEndDate: OffsetDateTime): List<PlanExportExcelModel> {
        val planProductIds = planProducts.mapNotNull { x -> x.id }
        val planProcesses = planProcessRep.getListPlanProcess(planProductIds)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }.sortedBy { x -> x.planProductId }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)


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
                    inventory = x.inventory,
                    unit = x.unit
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
                planData.add(PlanDataByProcessModel(title = ProcessPlan.PROCESS, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))
                productPlan.planData = planData
                productPlan
            }.sortedBy { x -> x.processSequence }
            data.add(productExport)
        }

        return data
    }


    fun importExcel(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val equipmentProductivity = EquipmentProductivity(
                processCode = ExcelHelper.getCellValue(row, 1).let { if (it.length > 6) it.substring(0, 6) else it },
                equipmentCode = "eCode",
                mold = ExcelHelper.getCellValue(row, 2),
                task = BigDecimal(ExcelHelper.getCellValue(row, 6)),
                time = ExcelHelper.getCellValue(row, 4).run { if (endsWith(".0")) substring(0, length - 2) else this }.toInt(),
                count = BigDecimal(ExcelHelper.getCellValue(row, 5)),
                setDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                blockDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 8)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                blockSh = BigDecimal(ExcelHelper.getCellValue(row, 8)),
                frame_1 = ExcelHelper.getCellValue(row, 0),
                sheetHour = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                sheetDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                operatingRate = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) * BigDecimal(100)
                ),
                sheetHour_100 = truncateDecimal(BigDecimal(ExcelHelper.getCellValue(row, 7))),
                description = "insert"
            )

            equipmentProductivityRepository.add(equipmentProductivity)
            count++
        }
        return BaseResponse(null, CommonUtils.getMessage("Insert Ok", arrayOf(count, total + 1)))
    }

    fun exportExcel(
    request: PlanSearchRequest,
    @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
    pageable: Pageable) : FileContentModel{
        var colStartDate = OffsetDateTime.now()
        var colEndDate = OffsetDateTime.now()
        if (request.filterType == OrderFilterType.DATE) {
            if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))
            colStartDate = request.startDate
            colEndDate = request.endDate
        }
        if (request.filterType == OrderFilterType.ORDER) {
            val plan = planRepository.getPlanByOrderCode(request.orderCode ?: "")
                ?: throw BusinessException(CommonUtils.getMessage("plan.notExistInOrder"))
            colStartDate = plan.startDate
            colEndDate = plan.endDate
        }

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        val columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)

        val dataExports = getPaginatedEquipmentProductivityPlan(request,pageable)
        if (dataExports?.data?.isEmpty() == true) throw BusinessException(CommonUtils.getMessage("plan.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportEqProdPlanTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val headerRow = sheet.getRow(0)
        var headerCol = 5
        val headerStyle = headerRow.getCell(1).cellStyle
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }
        val planDetails = dataExports?.data

        val style = ExcelHelper.getCellStyleCommon(workbook)
        var rowNumber = 1

        if (planDetails != null) {
            val groupedByFrame1: Map<String?, List<EquipmentProductivityModel>> = planDetails.groupBy { it.frame1 }

            groupedByFrame1.forEach { (frame1Value, frame1List) ->
                rowNumber = frame1Value?.let { generateExcelRowFrame(workbook, sheet, rowNumber, style, it) }!!
                for (planProcess in frame1List) {
                    generateExcelRowProcess(workbook, sheet, rowNumber, style, planProcess)
                    for (processDetail in planProcess.processDetail!!) {
                        rowNumber = generateExcelRowPlanData(workbook, sheet, rowNumber, style, columns, processDetail)
                    }
                }
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportPlanEquipment", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }


    private fun generateExcelRowFrame(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: String
    ):Int{
        var rowIndex = rowNumber
        val processNameDataRow = sheet.getRow(rowIndex++) ?: sheet.createRow(rowIndex++)
        ExcelHelper.setCellValueCustom(workbook, processNameDataRow, 0, style, data,isBold = true)
        return rowIndex

    }

    private fun generateExcelRowProcess(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: EquipmentProductivityModel
    ) {
        var rowIndex = rowNumber

        val processNameDataRow = sheet.getRow(rowIndex++) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(workbook, processNameDataRow, 1, style, data.processName,isBold = true,isAlignCenter = true)

        val processNameJpDataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(workbook, processNameJpDataRow, 1, style, data.processNameJp,isBold = true,isAlignCenter = true)
        rowIndex++

        val processConvertCodeDataRow = sheet.getRow(rowIndex++) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(workbook, processConvertCodeDataRow, 1, style, data.processConvertCode,isBold = true,isAlignCenter = true)
    }
    private fun generateExcelRowPlanData(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        columns: List<CalendarResponse>,
        data: ProcessDetailModel
    ): Int {

        var rowIndex = rowNumber
        val fixRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        val fixRowSecond = sheet.getRow(rowIndex+1) ?: sheet.createRow(rowIndex)
        val fixRowThird = sheet.getRow(rowIndex+2) ?: sheet.createRow(rowIndex)

        ExcelHelper.setCellValueCustom(workbook, fixRow, 2, style, data.name,isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,isAlignCenter = true,isBold = true)
        ExcelHelper.setCellValueCustom(workbook, fixRowSecond, 2, style, "",isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false)
        ExcelHelper.setCellValueCustom(workbook, fixRowThird, 2, style, "",isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = true)

        ExcelHelper.setCellValueCustom(workbook, fixRow, 3, style, data.totalProcess.toString(),isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,isAlignCenter = true,isBold = true)
        ExcelHelper.setCellValueCustom(workbook, fixRowSecond, 3, style, "",isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false)
        ExcelHelper.setCellValueCustom(workbook, fixRowThird, 3, style, "",isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = true)
        for (planData in data.processDetailList) {
            val dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            ExcelHelper.setCellValueCustom(workbook, dataRow, 4, style, planData.type,isBorderBottom = true,isBold = false)
            var colIndex = 5
            for (col in columns) {
                val value = planData.quantityByCalendars.find { x -> x.key == col.key }?.value
                ExcelHelper.setCellValueWithCalendar(workbook, dataRow, colIndex, style, value, col.isHoliday)
                colIndex++
            }
            rowIndex++
        }
        return rowIndex
    }
}