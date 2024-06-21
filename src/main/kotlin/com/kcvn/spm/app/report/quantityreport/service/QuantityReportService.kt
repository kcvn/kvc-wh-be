package com.kcvn.spm.app.report.quantityreport.service
import com.kcvn.spm.app.product.payload.model.ProcessGroupModel
import com.kcvn.spm.app.report.quantityreport.payload.model.ErrorOrderDetail
import com.kcvn.spm.app.report.quantityreport.payload.model.ProductNameAndLstProcess
import com.kcvn.spm.app.report.quantityreport.payload.model.ProductOrderDateKeyModel
import com.kcvn.spm.app.report.quantityreport.payload.model.ProductProcessKeyModel
import com.kcvn.spm.app.report.quantityreport.payload.model.QuantityReportModel
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityOfProcessRequest
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.report.quantityreport.payload.request.QuantityReportRequest
import com.kcvn.spm.app.report.quantityreport.payload.response.CheckCalculateQuantityResponse
import com.kcvn.spm.app.report.quantityreport.payload.response.InformationCalculateQuantityResponse
import com.kcvn.spm.app.report.quantityreport.payload.response.PagingQuantityReportResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantity
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantityDetail
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.repository.CalculateQuantityReportRepository
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.ProcessGroupRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.QuantityReportRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
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
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.round


@Service
@Transactional
class QuantityReportService(
    private val completionRateProductRepository: CompletionRateProductRepository,
    private val orderInfoRep : OrderInfoRepository,
    private val productRep: ProductRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val productProcessRep: ProductProcessRepository,
    private val calculateQuantityReportRep: CalculateQuantityReportRepository,
    private val quantityReportRep: QuantityReportRepository,
    private val processGroupRep: ProcessGroupRepository,

) {
    fun calculateQuantity(request: CalculateQuantityRequest): BaseResponse<FileContentModel?> {
        val calculateQuantityReport = calculateQuantityReportRep.findByMonthReport(request)
        return if (calculateQuantityReport != null && calculateQuantityReport.status == true) {
            BaseResponse(data = null, message = CommonUtils.getMessage("calculated.locked.error"))
        } else {
            // Validate ngày yêu cầu đơn hàng với tháng báo cáo

            val monthStartDate = request.startDate?.plusHours(7)?.monthValue ?: 0
            val monthEndDate = request.endDate?.plusHours(7)?.monthValue ?: 0
            val yearStartDate = request.startDate?.plusHours(7)?.year ?: 0
            val yearEndDate = request.endDate?.plusHours(7)?.year ?: 0
            val monthReport = request.monthReport
            val yearReport = request.yearReport
            if(request.monthReport == 1){
                if(!((monthStartDate != 12 && monthStartDate == 1 && yearStartDate == yearReport)
                            || (monthStartDate == 12 && yearReport!! - yearStartDate == 1))){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
                }
            }else {
                if(!((monthReport!! - monthStartDate == 1 || monthReport == monthStartDate) && yearReport == yearStartDate)){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
                }
            }
            if(monthReport == 12){
                if(!((monthEndDate != 1 && monthEndDate == 12 && yearEndDate == yearReport)
                            || (monthEndDate == 1 && yearEndDate - yearReport == 1))){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
                }
            }else {
                if(!((monthEndDate - (monthReport ?: 0)  == 1 || monthReport == monthEndDate) && yearReport == yearEndDate)){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
                }
            }
            // Validate thời gian yêu cầu của các tháng báo cáo phải là liên tiếp

            val monthPre: Int
            val yearPre: Int
            val monthNext: Int
            val yearNext: Int
            // Kiểm tra xem tháng của import vào trường hợp đặc biệt tháng 12 và 1 thì phải sang năm mới

            if(monthReport == 1){
                monthPre = 12
                yearPre = yearReport - 1
            }else {
                monthPre = monthReport!! - 1
                yearPre = yearReport
            }
            if(monthReport == 12){
                monthNext = 1
                yearNext = yearReport + 1
            }else {
                monthNext = monthReport + 1
                yearNext = yearReport
            }
            // check xem có tồn tại dữ liệu của tháng trước không

            val queryCalculateQuantityReportPre = calculateQuantityReportRep.getCalculateQuantityResultByMonthReport(monthPre,yearPre)
            if(queryCalculateQuantityReportPre != null){
                //val subtraction = request.startDate?.plusHours(7)!!.dayOfMonth.until(queryTapePre.requestDateEnd!!.dayOfMonth)
                val dayQueryTapePre = queryCalculateQuantityReportPre.endDate?.plusHours(7)?.toLocalDate()
                val dayReport = request.startDate?.plusHours(7)?.toLocalDate()

                if((ChronoUnit.DAYS.between(dayReport,dayQueryTapePre).absoluteValue != 1L)){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.orderRequestDate"))
                }
            }
            // check xem có tồn tại dữ liệu của tháng sau không

            val queryCalculateQuantityReportNext = calculateQuantityReportRep.getCalculateQuantityResultByMonthReport(monthNext,yearNext)
            if(queryCalculateQuantityReportNext != null){
                val dayQueryTapeNext = queryCalculateQuantityReportNext.startDate?.plusHours(7)?.toLocalDate()
                val dayReport = request.endDate?.plusHours(7)?.toLocalDate()
                if(ChronoUnit.DAYS.between(dayQueryTapeNext, dayReport).absoluteValue != 1L){
                    throw BusinessException(CommonUtils.getMessage("validate.importTape.orderRequestDate"))
                }
            }

            val listCalculateQuantityProcess = mutableListOf<CalculateQuantityOfProcessRequest>()
            val listOrderDetailError = mutableListOf<ErrorOrderDetail>()


            // Nếu mà có bản ghi tồn tại thì xóa bản ghi cũ đi và thêm lại tính sản lượng mới
            if(calculateQuantityReport != null) {
                val listIdInformationCalculateQuantity = calculateQuantityReportRep.getIdInformationCalculateQuantity(calculateQuantityReport.id)
                val listIdInformationCalculateQuantityDetail = calculateQuantityReportRep.getIdInformationCalculateQuantityDetail(listIdInformationCalculateQuantity)
                calculateQuantityReportRep.deleteInformationCalculateQuantityDetail(listIdInformationCalculateQuantityDetail)
                calculateQuantityReportRep.deleteInformationCalculateQuantity(calculateQuantityReport.id)
                calculateQuantityReportRep.deleteByIdReport(calculateQuantityReport.id)
            }
            // lấy ra chi tiết order dựa vào sản phẩm có version cao nhất và tháng tính sản lượng
            val orderDetails = orderInfoRep.getOrderInfoByTimeRange(request.startDate, request.endDate)

            val productNames = orderDetails.map { x -> x.productName }.distinct()
            val dataNonExistentProductsInDB
                    = completionRateProductRepository.getNonExistentProductsInDB(orderDetails, request.startDate)
            if(dataNonExistentProductsInDB.isNotEmpty()){
                val dataErr = exportExcelErr(dataNonExistentProductsInDB)
                return BaseResponse(data = dataErr, message = CommonUtils.getMessage("validate.quantityReportErr"))
            }
            val productsWithRate = productRep.getProductDetailWithCompletionRateByNames(productNames)
            //val productNames = productsWithRate.mapNotNull { x -> x?.name }.distinct()

            val productProcedureStructures = processProcedureStructureRep.getByProductName(productNames)
            val procedureStructureIds = productProcedureStructures.mapNotNull { x -> x.id }
            val productProcesses = productProcessRep.getByProcessProcedureStructure(procedureStructureIds)
            val productProcessGroups = productProcesses.filter { x ->
                !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
            }.map { x ->
                val procedureStructure = productProcedureStructures.find { m -> m.id == x.processProcedureStructureId }
                if (procedureStructure == null) ProcessGroupModel()
                else ProcessGroupModel(procedureStructure.productCode, x.processStatisticCode)
            }.filter { x -> !x.processStatisticCode.isNullOrEmpty() && !x.productName.isNullOrEmpty() }
                .groupBy { x -> Pair(x.productName, x.processStatisticCode) }

            val lstProductProcess = productNames.map { x ->
                val lstProcess = productProcessGroups.filter { m -> m.key.first == x }
                    .mapNotNull { m -> KeyValueResponse(m.key.second, m.value.size.toString()) }
                ProductNameAndLstProcess(
                    productName = x,
                    lstProcess = lstProcess
                )
            }


            val listOrderDetailCalculate = orderDetails.map { x ->
                //val ord = listOrderWithHighestVersion.find { m -> m.id == x.orderId }
                val prods = productsWithRate.filter { m -> m?.name == x.productName }

                val prod = prods.firstOrNull()

                val tld =
                    prods.filter { m -> m?.effectiveDate != null && m.effectiveDate!! <= x.orderDate }
                        .sortedByDescending { m -> m?.effectiveDate }
                        .firstOrNull()

                if (tld == null) {
                    listOrderDetailError.add(
                        ErrorOrderDetail(
                            productId = prod?.id,
                            productName = prod?.name,
                            orderDate = x.orderDate,
                        )
                    )
                    CalculateQuantityOfProcessRequest()
                } else {
                    CalculateQuantityOfProcessRequest(
                        version = x.version,
                        productName = prod?.name,
                        blockSh = prod?.shBlock,
                        quantityBlock = x.quantity,
                        completionRate = tld.rate?.toBigDecimal(),
                        orderDate = x.orderDate,
                        effectiveDate = tld.effectiveDate,
                        expirationDate = tld.expirationDate,
                    )
                }
            }.filter { x -> !x.productName.isNullOrEmpty() }

            listOrderDetailCalculate.forEach { x ->
                run {
                    val listProductProcess = lstProductProcess.find { y -> y.productName == x.productName }?.lstProcess
                    listProductProcess?.forEach { z ->
                        run {
                            listCalculateQuantityProcess.add(
                                CalculateQuantityOfProcessRequest(
                                    version = x.version,
                                    productName = x.productName,
                                    blockSh = x.blockSh,
                                    quantityBlock = x.quantityBlock,
                                    completionRate = x.completionRate,
                                    orderDate = x.orderDate,
                                    effectiveDate = x.effectiveDate,
                                    expirationDate = x.expirationDate,
                                    processStatisticCode = z.key,
                                    processCount = z.value?.toInt(),
                                )
                            )
                        }
                    }
                }
            }

            val listInformationCalculateQuantityDetails = listCalculateQuantityProcess.map { x ->
                InformationCalculateQuantityDetail(
                    monthReport = x.orderDate,
                    productName = x.productName,
                    processStatisticCode = x.processStatisticCode,
                    orderDate = x.orderDate,
                    completionRate = x.completionRate,
                    processCount = x.processCount,
                    blockQuantity = x.quantityBlock,
                    blockSh = x.blockSh,
                    quantityProcessStatistic = if (x.completionRate!!.toInt() == 0) {
                        0
                    } else {
                        round(((x.processCount!! * x.quantityBlock!!) / (x.blockSh!!.times(x.completionRate.toDouble()) / 100))).toInt()
                    },
                    createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM,
                    monthNumber = request.monthReport,
                    yearNumber = request.yearReport,
                )
            }

            val data = listInformationCalculateQuantityDetails.groupBy {
                ProductProcessKeyModel(
                    it.productName,
                    it.processStatisticCode
                )
            }

            val listInformationQuantity = data.map { (key, items) ->
                InformationCalculateQuantity(
                    monthReport = items.first().monthReport,
                    productName = key.productName,
                    processStatistic = key.processStatisticCode,
                    totalQuantityOfProcess = items.sumOf { it.quantityProcessStatistic!! },
                    createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM,
                    monthNumber = request.monthReport,
                    yearNumber = request.yearReport,
                )
            }


            val quantityResult = CalculateQuantityResult(
                monthReport = request.startDate,
                orderDateFromTo = "${
                    request.startDate?.plusHours(7)?.let {
                        DateTimeHelper.toString(
                            it,
                            DateTimeFormat.dd_MM_yyyy
                        )
                    }
                }-${
                    request.endDate?.let {
                        DateTimeHelper.toString(
                            it, DateTimeFormat.dd_MM_yyyy
                        )
                    }
                }",
                startDate = request.startDate,
                endDate = request.endDate,
                calculateBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM,
                calculateDate = OffsetDateTime.now(),
                monthNumber = request.monthReport,
                yearNumber = request.yearReport,

            )

            calculateQuantityReportRep.addCalculateQuantityResult(
                quantityResult,
                listInformationQuantity,
                listInformationCalculateQuantityDetails
            )

            BaseResponse(data = null, message = CommonUtils.getMessage("calculated.success"))
        }
    }

    fun lockedQuantity(request: String): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findById(request)
        calculateQuantityReport!!.status = true
        calculateQuantityReportRep.update(calculateQuantityReport)
        return BaseResponse(true, message = CommonUtils.getMessage("quantity.locked.success"))
    }

    fun getListCalculateQuantityResult(pageable: Pageable): BasePagingResponse<CalculateQuantityResult> {
        val calculateQuantityResults = calculateQuantityReportRep.getPagingListCalculateQuantityResult(pageable)
        var response = BasePagingResponse<CalculateQuantityResult>()

        if (calculateQuantityResults.first.isNotEmpty()) {
            response = mappingWorkResultResponse(calculateQuantityResults.first)
            response.totalRecords = calculateQuantityResults.second
        }
        return response
    }

    private fun mappingWorkResultResponse(calculateQuantityResults: List<CalculateQuantityResult>): BasePagingResponse<CalculateQuantityResult> {
        val response = BasePagingResponse<CalculateQuantityResult>()
        response.data = calculateQuantityResults.map { x ->
            CalculateQuantityResult(
                id = x.id,
                monthReport = x.monthReport,
                startDate = x.startDate,
                endDate = x.endDate,
                status = x.status,
                calculateBy = x.calculateBy,
                calculateDate = x.calculateDate,
                lockedBy = x.lockedBy,
                lockedDate = x.lockedDate,
                monthNumber = x.monthNumber,
                yearNumber = x.yearNumber
            )
        }
        return response
    }

    fun getListQuantityReport(request: QuantityReportRequest?, pageable: Pageable): PagingQuantityReportResponse {
        val informationCalculateQuantity = quantityReportRep.getPagingListQuantityReport(request, pageable)
        var response = PagingQuantityReportResponse()

        if (informationCalculateQuantity.first.isNotEmpty()) {
            response = mappingInformationCalculateQuantityResponse(informationCalculateQuantity.first)
        }
        return response
    }

    private fun mappingInformationCalculateQuantityResponse(data: List<InformationCalculateQuantityResponse>): PagingQuantityReportResponse {
        val group = data.groupBy {
            ProductOrderDateKeyModel(
                it.productName,
                it.monthNumber,
                it.yearNumber,
                it.orderDateFromTo
            )
        }

        val listQuantityReportModel = group.map { x ->
            QuantityReportModel(
                x.key.productName,
                x.key.monthNumber,
                x.key.yearNumber,
                x.key.orderDateFromTo,
                x.value.map { m -> KeyValueResponse(m.processStatistic, m.totalQuantityOfProcess.toString()) }.toMutableList()
            )
        }

        val productNames = listQuantityReportModel.mapNotNull { x -> x.productName }.distinct()
        val productProcedureStructures = processProcedureStructureRep.getByProductName(productNames)
        val procedureStructureIds = productProcedureStructures.mapNotNull { x -> x.id }
        val productProcesses = productProcessRep.getByProcessProcedureStructure(procedureStructureIds)
        val processGroups = processGroupRep.getForProduct()

        val columns = productProcesses.asSequence().filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
        }.map { x ->
            val processGroup =
                processGroups.find { m -> !m.processStatisticCode.isNullOrEmpty() && m.processStatisticCode == x.processStatisticCode }
            if (processGroup == null) KeyValueResponse()
            else KeyValueResponse(x.processStatisticCode, processGroup.description, processGroup.sortOrder)
        }.filter { x -> !x.key.isNullOrEmpty() && !x.value.isNullOrEmpty() }.distinct().toMutableList()

        if (columns.any { m -> m.key == ProcessStatisticCode.HP_TAN || m.key == ProcessStatisticCode.HP_ALL }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.IN_LO }
            if (processGroup != null) columns.add(
                KeyValueResponse(
                    processGroup.processStatisticCode,
                    processGroup.description,
                    processGroup.sortOrder
                )
            )
        }
        if (columns.any { m -> m.key == ProcessStatisticCode.TAN || m.key == ProcessStatisticCode.ZEN }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.IN_MACH }
            if (processGroup != null) columns.add(
                KeyValueResponse(
                    processGroup.processStatisticCode,
                    processGroup.description,
                    processGroup.sortOrder
                )
            )
        }
        if (columns.any { m -> m.key == ProcessStatisticCode.M_TAN || m.key == ProcessStatisticCode.M_ALL }) {
            val processGroup = processGroups.find { m -> m.processStatisticCode == ProcessStatisticCode.GHEP_LOP }
            if (processGroup != null) columns.add(
                KeyValueResponse(
                    processGroup.processStatisticCode,
                    processGroup.description,
                    processGroup.sortOrder
                )
            )
        }

        val response = PagingQuantityReportResponse()
        listQuantityReportModel.forEach { x ->
            run {
                val inlo = ((x.lstProcess.find { m -> m.key == ProcessStatisticCode.HP_TAN }?.value?.toInt() ?: 0 ) +
                        (x.lstProcess.find { m -> m.key == ProcessStatisticCode.HP_ALL }?.value?.toInt() ?: 0)).toString()
                val rsInlo = KeyValueResponse(
                    key = ProcessStatisticCode.IN_LO,
                    value = inlo,
                )
                x.lstProcess.add(rsInlo)

                val inmach = ((x.lstProcess.find { m -> m.key == ProcessStatisticCode.TAN }?.value?.toInt() ?: 0 ) +
                        (x.lstProcess.find { m -> m.key == ProcessStatisticCode.ZEN }?.value?.toInt() ?: 0)).toString()
                val rsInmach = KeyValueResponse(
                    key = ProcessStatisticCode.IN_MACH,
                    value = inmach,
                )
                x.lstProcess.add(rsInmach)

                val gheplop = ((x.lstProcess.find { m -> m.key == ProcessStatisticCode.M_TAN }?.value?.toInt() ?: 0 ) +
                        (x.lstProcess.find { m -> m.key == ProcessStatisticCode.M_ALL }?.value?.toInt() ?: 0)).toString()
                val rsGheplop = KeyValueResponse(
                    key = ProcessStatisticCode.GHEP_LOP,
                    value = gheplop,
                )
                x.lstProcess.add(rsGheplop)
            }
        }



        response.data = listQuantityReportModel
        response.columns = columns.sortedBy { x -> x.sort }.distinct().toList()
        response.totalRecords = listQuantityReportModel.count()
        return response
    }

    fun checkCalculateQuantity (request: CalculateQuantityRequest) : CheckCalculateQuantityResponse  {
        val data = CheckCalculateQuantityResponse()
        val query = calculateQuantityReportRep.findByMonthReport(request)
        if(query != null){
            if(query.status == true){
                throw BusinessException(CommonUtils.getMessage("calculated.locked.error"))
            }
            data.hasCalculateQuantity = true
            return data
        }
        return data
    }


    fun setCellHeader(workbook: Workbook, row: Row, colIndex: Int, style: CellStyle, value: String?) {
        row.createCell(colIndex).setCellValue(value)
        row.getCell(colIndex).cellStyle = style
    }

    private fun generateExcelRowPlan(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: QuantityReportModel,
        columns: List<KeyValueResponse>
    ): Int {
        val numberStyle = workbook.createCellStyle()
        numberStyle.cloneStyleFrom(style)
        numberStyle.alignment = HorizontalAlignment.RIGHT

        var rowIndex = rowNumber

        val rowPlan = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(rowPlan, 0, style, data.productName)
        ExcelHelper.setCellValue(rowPlan, 1, style, "${data.monthNumber.toString()}/${data.yearNumber.toString()}")
        ExcelHelper.setCellValue(rowPlan, 2, style, data.orderDateFromTo)
        var colIndex = 3
        for (col in columns) {
            val value = data.lstProcess.find { x -> x.key == col.key }?.value
            ExcelHelper.setCellValue(rowPlan, colIndex, numberStyle, NumberHelper.formatNumber(value?.toIntOrNull()))
            colIndex++
        }
        rowIndex++
       return rowIndex
    }


    fun exportQuantityReportExcel(request: QuantityReportRequest?, pageable: Pageable) : BaseResponse<FileContentModel>{
        val dataExport = getListQuantityReport(request,pageable)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportQuantityReportTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)
        var headerCol = 3
        val headerStyle = ExcelHelper.setCellHeaderStyle(workbook)
        val columns = dataExport.columns
        if (dataExport.data != null){
            if (columns != null) {
                for (col in columns) {
                    setCellHeader(workbook, headerRow, headerCol, headerStyle, col.value)
                    headerCol++
                }
            }
            var rowNumber = 1
            val style = ExcelHelper.getCellStyleCommon(workbook)
            style.alignment = HorizontalAlignment.CENTER

            for(report in dataExport.data!!){
                if (columns != null) {
                    rowNumber = generateExcelRowPlan(workbook, sheet, rowNumber, style, report,columns)
                }
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportQuantityReport", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )
        workbook.close()
        return BaseResponse(response)
    }

    fun exportExcelErr(productNames: List<OrderInfo?>) : FileContentModel{
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportRateCalculateQuantityErr.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)


        val style = ExcelHelper.getCellStyleCommon(workbook)
        // Tạo một CellStyle mới
        val redFontStyle = workbook.createCellStyle()

        // Tạo một Font mới
        val redFont = workbook.createFont()

        // Đặt màu chữ là đỏ
        redFont.color = IndexedColors.RED.getIndex()
        redFont.fontName = ExcelConstant.FONT_TIMES_NEW_ROMAN
        redFont.fontHeightInPoints = 12.toShort()
        // Đặt font cho CellStyle
        redFontStyle.setFont(redFont)
        redFontStyle.borderBottom = BorderStyle.THIN
        redFontStyle.borderTop = BorderStyle.THIN
        redFontStyle.borderRight = BorderStyle.THIN
        redFontStyle.borderLeft = BorderStyle.THIN
        redFontStyle.wrapText = true
        redFontStyle.verticalAlignment = VerticalAlignment.CENTER

        var rowNumber = 1
        for (item in productNames) {
            val dataRow: Row = sheet.createRow(rowNumber++)
            ExcelHelper.setCellValue(dataRow, 0, style, item?.productName)
            ExcelHelper.setCellValue(dataRow, 1, redFontStyle, CommonUtils.getMessage("validate.rateQuantityReport", arrayOf(
                item?.orderDate?.plusHours(7)!!.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")))))
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportRateQuantityReportErr", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return response
    }
}