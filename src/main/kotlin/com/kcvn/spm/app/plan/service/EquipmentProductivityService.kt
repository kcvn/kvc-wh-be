package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.*
import com.kcvn.spm.app.plan.payload.request.PlanProcessDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
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
import com.kcvn.spm.model.tables.pojos.PlanProcess
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
    private val holidaysCalenderRep: HolidaysCalenderRepository
) {

    fun getPaginatedEquipmentProductivityPlan(
        request: PlanSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): PagingEquipmentProdResponse? {

        val listPlan = planProductRep.getListPlanProduct(request, pageable)
        val listPlanIds: List<String> = listPlan.first.map { plan -> plan.id.toString() }
        val requestListPlan = PlanProcessDetailRequest(listPlanIds, request.filterType, request.startDate, request.endDate, request.orderCode)
        val result = getPlanDetail(requestListPlan)
        return result

    }


    fun getPlanDetail(request: PlanProcessDetailRequest): PagingEquipmentProdResponse {
        val response = PagingEquipmentProdResponse()
        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        if (request.planProductId.isNullOrEmpty()) throw BusinessException("")
        val colStartDate: OffsetDateTime
        val colEndDate: OffsetDateTime
        when (request.filterType) {
            OrderFilterType.DATE -> {
                if (request.startDate == null || request.endDate == null) throw BusinessException("")
                colStartDate = request.startDate ?: OffsetDateTime.now()
                colEndDate = request.endDate ?: OffsetDateTime.now()
            }
            OrderFilterType.ORDER -> {
                val plan = request.orderCode?.let { planRepository.getPlanByOrderCode(it) } ?: throw BusinessException("")
                colStartDate = plan.startDate ?: OffsetDateTime.now()
                colEndDate = plan.endDate ?: OffsetDateTime.now()
            }
            else -> throw BusinessException("")
        }
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)
        val plan: MutableList<EquipmentProductivityModel> = mutableListOf()
        for(planProductId in request.planProductId!!){
            val planProduct = planProductRep.getById(planProductId)
            val planProcesses = planProcessRep.getListPlanProcess(planProductId)
            val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
            val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
            val planDetails = planDetailRep.getPlanDetail(planProcessIds)
            if(planProduct!=null){
                for(planProcess in parentPlanProcess){
                    val equipmentProductivity = EquipmentProductivityModel(
                        frame1 = planProduct.frame_1,
                        processName = planProcess.processName,
                        processNameJp= planProcess.processNameJp,
                        processConvertCode = planProcess.processConvertCode,
                        processCode = planProcess.processCode

                    )
                    val planDetailByProcess = planDetails.filter { m -> m.planProcessId == planProcess.id }
                    val processDetailListModel =  mutableListOf<ProcessDetailListModel>()
                    //process
                    processDetailListModel.add(
                        ProcessDetailListModel(
                            type = ProcessPlan.PROCESS,
                            quantityByCalendars = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_KEY }.map { t ->
                                KeyValueResponse(
                                    DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                                    if (planProcess.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                                )
                            }.toMutableList()
                        )
                    )

                    //end
                    val totalProcessValue: Int = processDetailListModel.sumOf { processDetail ->
                        processDetail.quantityByCalendars.sumOf { keyValueResponse ->
                            keyValueResponse.value?.toIntOrNull() ?: 0
                        }
                    }
                    val processDetailList = mutableListOf<ProcessDetailModel>()
                    processDetailList.add(
                        ProcessDetailModel(
                            name=planProduct.mold,
                            totalProcess = totalProcessValue,
                            processDetailList = processDetailListModel
                        )
                    )
                    equipmentProductivity.processDetail = processDetailList
                    plan.add(equipmentProductivity)
                }
            }
        }
        // logic
        val groupedData = plan.groupBy {
            listOf(
                it.frame1,
                it.processName,
                it.processNameJp,
                it.processConvertCode
            )
        }
        val mergedData = groupedData.values.map { group ->
            val mergedEquipmentModel = EquipmentProductivityModel(
                frame1 = group.firstOrNull()?.frame1,
                processName = group.firstOrNull()?.processName,
                processNameJp = group.firstOrNull()?.processNameJp,
                processConvertCode = group.firstOrNull()?.processConvertCode,
                processCode = group.firstOrNull()?.processCode,
                processDetail = mutableListOf()
            )
            val mergedProcessDetails = mutableMapOf<String, MutableList<ProcessDetailListModel>>()
            group.forEach { equipmentModel ->
                equipmentModel.processDetail?.forEach { processDetail ->
                    val name = processDetail.name ?: ""
                    val existingDetail = mergedProcessDetails.getOrPut(name) { mutableListOf() }
                    processDetail.processDetailList.forEach { detailItem ->
                        val existingItem = existingDetail.find { it.type == detailItem.type }
                            ?: ProcessDetailListModel(type = detailItem.type, quantityByCalendars = mutableListOf()).also {
                                existingDetail.add(it)
                            }
                        detailItem.quantityByCalendars.forEach { keyValueResponse ->
                            val existingKeyValue = existingItem.quantityByCalendars.find { it.key == keyValueResponse.key }
                                ?: KeyValueResponse(key = keyValueResponse.key, value = "").also {
                                    existingItem.quantityByCalendars.add(it)
                                }
                            if (detailItem.type == ProcessPlan.MACHINE) {
                                existingKeyValue.value = keyValueResponse.value
                            } else {
                                existingKeyValue.value = ((existingKeyValue.value?.toDoubleOrNull() ?: 0.0) + (keyValueResponse.value?.toDoubleOrNull() ?: 0.0)).toString()
                            }
                        }
                    }
                }
            }
            val mergedProcessDetailList = mergedProcessDetails.map { (name, detailList) ->
                val totalProcess = detailList.sumOf { processDetail ->
                    if (processDetail.type == ProcessPlan.PROCESS) {
                        processDetail.quantityByCalendars.sumOf { keyValueResponse ->
                            keyValueResponse.value?.toDoubleOrNull() ?: 0.0
                        }
                    } else { 0.0 }
                }
                ProcessDetailModel(
                    name = name,
                    totalProcess = totalProcess.toInt(),
                    processDetailList = detailList
                )
            }
            mergedEquipmentModel.processDetail = mergedProcessDetailList.toMutableList()
            mergedEquipmentModel
        }
        // Add process and process number

        addProcessAndProcessNumber(mergedData, response.columns!!)

        response.data = mergedData
        return response
    }


    private fun addProcessAndProcessNumber(data: List<EquipmentProductivityModel>,columns: List<CalendarResponse>) {
        val equipmentProductList = equipmentProductivityRepository.getEquipmentProductivity()

        for(equipmentProductivity in data){
            for(processDetailModel in equipmentProductivity.processDetail!!){
                val uniqueSheetDayValue: BigDecimal? = if(equipmentProductivity.processName.equals(ProcessPlan.PROCESS_DUC_LO)){
                    equipmentProductList
                        .firstOrNull { equipment ->
                            equipment.processCode.equals(equipmentProductivity.processCode) &&
                                    equipment.mold.equals(processDetailModel.name) &&
                                    equipment.frame_1.equals(equipmentProductivity.frame1)
                        }?.sheetDay
                } else{
                    equipmentProductList
                        .firstOrNull { equipment ->
                            equipment.processCode.equals(equipmentProductivity.processCode) &&
                                    equipment.frame_1.equals(equipmentProductivity.frame1)
                        }?.sheetDay
                }


                processDetailModel.processDetailList.add(
                        ProcessDetailListModel(
                            type = ProcessPlan.MACHINE,
                            quantityByCalendars = columns.map { column ->
                                KeyValueResponse(
                                    column.key,
                                    uniqueSheetDayValue?.toInt()?.toString() ?: ""

                                )
                            }.toMutableList()
                        )
                    )
                    // machine number
                    val processValues = processDetailModel.processDetailList
                        .firstOrNull { it.type == ProcessPlan.PROCESS }
                        ?.quantityByCalendars
                processDetailModel.processDetailList.add(
                        ProcessDetailListModel(
                            type = ProcessPlan.MACHINENUMBER,
                            quantityByCalendars = if (uniqueSheetDayValue != null) {
                                processValues!!.map { column ->
                                    val result = (column.value?.toDoubleOrNull() ?: 0.0) / uniqueSheetDayValue.toDouble()
                                    val formattedResult = String.format("%.1f", result)
                                    KeyValueResponse(
                                        column.key,
                                        formattedResult
                                    )
                                }.toMutableList()
                            } else {
                                processValues!!.map { column ->
                                    KeyValueResponse(column.key, "")
                                }.toMutableList()
                            }
                        )
                    )
                }

        }
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
            val groupedByFrame1: Map<String?, List<EquipmentProductivityModel>> = planDetails.groupBy { it.frame1 } ?: emptyMap()

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
            fileName = CommonUtils.getMessage("fileName.exportPlanEquipment ", arrayOf(
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
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
        val processNameDataRow = sheet.getRow(rowIndex++) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(workbook, processNameDataRow, 0, style, data)
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
        ExcelHelper.setCellValue(workbook, processNameDataRow, 1, style, data.processName)

        val processNameJpDataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(workbook, processNameJpDataRow, 1, style, data.processNameJp)
        rowIndex++

        val processConvertCodeDataRow = sheet.getRow(rowIndex++) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(workbook, processConvertCodeDataRow, 1, style, data.processConvertCode)
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
        ExcelHelper.setCellValue(workbook, fixRow, 2, style, data.name)
        ExcelHelper.setCellValue(workbook, fixRow, 3, style, data.totalProcess.toString())
        for (planData in data.processDetailList) {
            val dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            ExcelHelper.setCellValue(workbook, dataRow, 4, style, planData.type)
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