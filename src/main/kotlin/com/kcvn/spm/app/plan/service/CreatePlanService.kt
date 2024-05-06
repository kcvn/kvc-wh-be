package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.plan.payload.model.AllocationRateByDateModel
import com.kcvn.spm.app.plan.payload.model.PlanCalculatorModel
import com.kcvn.spm.app.plan.payload.model.PlanChildrenProcessCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanDetailCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanProcessCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanProductCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanTempModel
import com.kcvn.spm.app.plan.payload.model.PlanValidateModel
import com.kcvn.spm.app.plan.payload.request.CheckInventoryRequest
import com.kcvn.spm.app.plan.payload.request.CreatePlanRequest
import com.kcvn.spm.app.productprocess.payload.model.ProductProcessModel
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.Frame1
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.constants.PlanTitle
import com.kcvn.spm.common.constants.ProcessCode
import com.kcvn.spm.common.constants.ProcessConvertCode
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.constants.ProcessUnit
import com.kcvn.spm.common.constants.SR_OR_NSR
import com.kcvn.spm.common.constants.YesNoConfig
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.model.tables.pojos.PlanCalendarConfig
import com.kcvn.spm.model.tables.pojos.PlanDetailTemp
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
import com.kcvn.spm.model.tables.pojos.PlanTemp
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.AppSettingRepository
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.repository.EquipmentProductivityRepository
import com.kcvn.spm.repository.HolidaysCalenderRepository
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.PlanCalendarConfigRepository
import com.kcvn.spm.repository.PlanRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.SystemLockRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Service
@Transactional
class CreatePlanService(
    private val orderInfoRep: OrderInfoRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val productProcessRep: ProductProcessRepository,
    private val productRep: ProductRepository,
    private val completionRateProcessProductRep: CompletionRateProcessProductRepository,
    private val equipmentProductivityRep: EquipmentProductivityRepository,
    private val systemLockRep: SystemLockRepository,
    private val planRep: PlanRepository,
    private val planCalendarConfigRep: PlanCalendarConfigRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val workResultRep: WorkResultRepository,
    private val appSettingRep: AppSettingRepository
) {

    private val typeOfSystemLocks = listOf(
        Constants.SYSTEM_LOCK_CREATE_PLAN,
        Constants.SYSTEM_LOCK_IMPORT_ORDER,
        Constants.SYSTEM_LOCK_PRODUCT_PROCESS
    )

    private var checkExceptionInventoryDate = false
    private var applyEquipmentProductivity = false

    fun checkInventory(request: CheckInventoryRequest): BaseResponse<Boolean> {
        if (request.inventoryDate == null) {
            return BaseResponse(true)
        }
        val inventory = inventoryProductRep.findDateInventoryProduct(request.inventoryDate!!)
        if (inventory != null) return BaseResponse(true)
        return BaseResponse(
            false,
            "Không có thông tin tồn kho tại ngày " +
                "${DateTimeHelper.toString(DateTimeHelper.toTimeZone7(request.inventoryDate!!)!!, DateTimeFormat.dd_MM_yyyy)}.\n" +
                "Bạn có muốn tiếp tục tạo kế hoạch sản xuất không ?"
        )
    }

    //region VALIDATE
    fun createPlan(request: CreatePlanRequest): BaseResponse<FileContentModel> {
        if (systemLockRep.isLock(Constants.SYSTEM_LOCK_CREATE_PLAN))
            throw BusinessException("Chức năng này đang bị khóa tạm thời. Vui lòng thử lại sau ít phút nữa")

        val month = request.planMonth.split("/")[0].toInt()
        val year = request.planMonth.split("/")[1].toInt()
        val planCalendarConfig = planCalendarConfigRep.getConfigByMonth(month, year)
            ?: throw BusinessException("Chưa đăng ký tháng sản xuất")

        val startDate = planCalendarConfig.startDate!!
        val endDate = planCalendarConfig.endDate!!

        if (request.inventoryDate != null && (request.inventoryDate!!.isBefore(startDate) || request.inventoryDate!!.isAfter(endDate)))
            throw BusinessException("Ngày chốt tồn kho đang nằm ngoài khoảng thời gian của tháng sản xuất")

        val orderInfo = orderInfoRep.getOrderInfoByTimeRange(startDate, endDate, request.productNames)
        if (orderInfo.isEmpty())
            throw BusinessException("Không có dữ liệu xuất hàng " +
                "từ ngày ${DateTimeHelper.toString(startDate, DateTimeFormat.dd_MM_yyyy)} " +
                "đến ngày ${DateTimeHelper.toString(endDate, DateTimeFormat.dd_MM_yyyy)}"
            )

        systemLockRep.lock(typeOfSystemLocks)

        try {
            val productNames = orderInfo.mapNotNull { it.productName }.sortedBy { it }.distinct()
            val productShortcutNames = productNames.map { it.substring(it.length - 7, it.length) }
            val productInfo = productRep.getByName(productNames)
            val productProcesses = productProcessRep.getByProductName(productNames)
            val completionRateInfo = completionRateProcessProductRep.getByProductName(productShortcutNames)

            val processGroupCodes = productProcesses.mapNotNull { x -> x.processGroup }
            val equipmentInfo = equipmentProductivityRep.getByProcessGroup(processGroupCodes).map { item ->
                item.sltbBlock = (item.sltbBlock ?: BigDecimal(0)) * BigDecimal(item.quantityMachine ?: 0)
                item.sltbSheet = (item.sltbSheet ?: BigDecimal(0)) * BigDecimal(item.quantityMachine ?: 0)
                item
            }
            val processStatisticCodeChecks = listOf(ProcessStatisticCode.T, ProcessStatisticCode.GHEPLOP_GIAAPNHIET, ProcessStatisticCode.SNAP)

            val planValidates = mutableListOf<PlanValidateModel>()
            for (item in productNames) {
                val errors = mutableListOf<String>()
                val product = productInfo.firstOrNull { x -> x.name == item }
                if (product == null) errors.add("Không tồn tại thông tin sản phẩm")

                val productProcess = productProcesses.filter { x -> x.productName == item && x.processCode?.toIntOrNull() != 0 }.map { model ->
                    if (model.processConvertCode == ProcessConvertCode.TH) model.processGroup = "20500"
                    model
                }
                if (productProcess.isEmpty()) {
                    errors.add("Chưa có thông tin công đoạn")
                } else {
                    if (productProcess.any { x -> x.processConvertCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Mã chuyển đổi")
                    if (productProcess.any { x -> x.processStatisticCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Mã thống kê")
                    if (productProcess.any { x -> x.dayOfImplementation == null || x.dayOfImplementation == 0 }) errors.add("Dữ liệu công đoạn chưa đầy đủ Ngày thứ thực hiện")
                    if (productProcess.any { x -> x.unit.isNullOrEmpty() && x.processInventoryCode.isNullOrEmpty() }) errors.add("Dữ liệu công đoạn chưa đầy đủ Đơn vị tính")
                }
                val parentProcesses = productProcess.filter { x ->
                    !x.processStatisticCode.isNullOrEmpty() && x.processInventoryCode.isNullOrEmpty()
                        && x.processConvertCode != ProcessConvertCode.INS
                }
                val processNotCompletionRate = productProcess.filter { x ->
                    x.processInventoryCode.isNullOrEmpty()
                        && !completionRateInfo.any { m ->
                        m.productNameShortcut == item.substring(item.length - 7, item.length)
                            && m.processCode == x.processCode
                    }
                }
                if (processNotCompletionRate.isNotEmpty()) {
                    val strProcess = processNotCompletionRate.map { x -> x.processCode }.distinct().joinToString(separator = ", ")
                    errors.add("Chưa có thông tin tỷ lệ đạt của các công đoạn $strProcess")
                }

                val equipmentByProducts = equipmentInfo.filter { x -> x.frame_1 == product!!.frame_1 }
                if (equipmentByProducts.isEmpty()) {
                    errors.add("Chưa có cấu hình năng suất máy cho line ${product!!.frame_1}")
                } else {
                    val processGroups = parentProcesses.filter { it.processConvertCode != ProcessConvertCode.INS }.mapNotNull { x -> x.processGroup }.distinct()
                    for (grp in processGroups) {
                        val eqConfigs = equipmentByProducts.filter { x -> x.grpProcess == grp }
                        if (eqConfigs.isEmpty()) {
                            if (grp == "21800") continue
                            if (grp == "21500" && (product!!.frame_1 == Frame1.SWR || product.frame_1 == Frame1.ML)) continue
                            errors.add("Chưa có cấu hình năng suất máy cho nhóm công đoạn $grp")
                        }
                    }
                    for (process in parentProcesses) {
                        var eqConfigs = equipmentByProducts.filter { x -> x.grpProcess == process.processGroup }
                        if (eqConfigs.isNotEmpty()) {
                            if (product!!.frame_1 != Frame1.MU) continue
                            if (
                                processStatisticCodeChecks.contains(process.processStatisticCode)
                                || process.processConvertCode == ProcessConvertCode.DAN_2L
                                || process.processCode == ProcessCode.TKCSP
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

            if (planValidates.isEmpty()) {
                checkExceptionInventoryDate = (appSettingRep.findByKey(KeyAppSetting.CHECK_EXCEPTION_INVENTORY_DATE_WHEN_CREATE_PLAN)?.value ?: "") == YesNoConfig.YES
                applyEquipmentProductivity = (appSettingRep.findByKey(KeyAppSetting.APPLY_EQUIPMENT_PRODUCTIVITY_WHEN_CREATE_PLAN)?.value ?: "") == YesNoConfig.YES
                val holidays = holidaysCalenderRep.getHolidaysCalender()
                if (request.inventoryDate == null) {
                    createPlanNoInventory(request, planCalendarConfig, orderInfo, productInfo, productProcesses, completionRateInfo, equipmentInfo, holidays)
                } else {
                    val inventories = inventoryProductRep.getInventoryForCreatePlan(productNames, request.inventoryDate!!)
                    if (inventories.isEmpty()) {
                        createPlanNoInventory(request, planCalendarConfig, orderInfo, productInfo, productProcesses, completionRateInfo, equipmentInfo, holidays)
                    } else {
                        val orderInfoFilter = orderInfo.filter { x ->
                            x.orderDate!!.isEqual(request.inventoryDate) || x.orderDate!!.isAfter(request.inventoryDate)
                        }
                        if (orderInfoFilter.isEmpty()) throw BusinessException("Không có dữ liệu xuất hàng kể từ ngày chốt tồn kho")

                        val planProductData = calculatePlanProductWithInventory(
                            orderInfoFilter, productInfo, productProcesses,
                            completionRateInfo, equipmentInfo, inventories, holidays
                        )

                        val planDetailData = planProductData.map { x -> x.planProcesses.map { m -> m.planDetails }.flatten() }.flatten().sortedBy { x -> x.planDate }
                        if (checkExceptionInventoryDate && planDetailData.any { it.planDate!!.isBefore(request.inventoryDate) })
                            throw BusinessException("Ngày kế hoạch không được nhỏ hơn ngày chốt tồn kho")

                        val planStartDate = planDetailData.first().planDate!!
                        val planEndDate = planDetailData.last().planDate!!
                        val hasInventory = planDetailData.any { it.hasInventory == true }

                        val planDataTemp = planRep.getDataForRePlan(month, year)

                        if (request.replan == true) {
                            createPlanWithInventoryHasRePlan(
                                request, planCalendarConfig, planProductData, planDataTemp,
                                planStartDate, planEndDate, hasInventory
                            )
                        } else {
                            createPlanWithInventoryNoRePlan(
                                request, planCalendarConfig, planProductData, planDataTemp,
                                planStartDate, planEndDate, hasInventory
                            )
                        }
                    }
                }
                return BaseResponse(message = "Tạo kế hoạch thành công")
            }

            val fileContent = exportFilePlanValidate(planValidates)
            return BaseResponse(fileContent)

        } catch (e: Exception) {
            throw e
        } finally {
            systemLockRep.unlock(typeOfSystemLocks)
        }
    }

    private fun exportFilePlanValidate(planValidates: List<PlanValidateModel>): FileContentModel {
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

    //endregion

    //region CREATE_PLAN_NO_INVENTORY
    private fun createPlanNoInventory(
        request: CreatePlanRequest,
        planCalendarConfig: PlanCalendarConfig,
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        productProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        equipmentInfo: List<EquipmentProductivity>,
        holidays: List<OffsetDateTime>
    ) {
        val planProductMappings = calculatePlanProductNoInventory(
            orderInfo, productInfo, productProcesses,
            completionRateInfo, equipmentInfo, holidays
        )

        val data = planProductMappings.map { x -> x.planProcesses.map { m -> m.planDetails }.flatten() }.flatten().sortedBy { it.planDate }
        val startDate = data.first().planDate!!
        val endDate = orderInfo.sortedByDescending { x -> x.orderDate }.first().orderDate!!
        val hasInventory = data.any { it.hasInventory == true }
        planRep.createPlanTemp(generatePlanModel(request, planCalendarConfig, startDate, endDate, hasInventory), planProductMappings)
    }

    private fun calculatePlanProductNoInventory(
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        productProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        equipmentInfo: List<EquipmentProductivity>,
        holidays: List<OffsetDateTime>
    ): MutableList<PlanProductCreateModel> {
        val productGroupByDates = sortProduct(orderInfo, productInfo, listOf(), productProcesses)
        val planProducts = mutableListOf<PlanProductCreateModel>()

        var equipmentUsedInfoByDate = mutableListOf<Pair<OffsetDateTime, EquipmentProductivity>>()
        for (iProdByDate in productGroupByDates) {
            val orders = iProdByDate.second
            for (iOrder in orders) {
                val product = productInfo.firstOrNull { x -> x.name == iOrder.productName }
                val processes = productProcesses.filter { x -> x.productName == iOrder.productName }
                var processSource = processes.firstOrNull { x -> x.processConvertCode == ProcessConvertCode.INS }

                if (processes.isEmpty() || product == null || processSource == null) continue

                val completionRates = completionRateInfo.filter { x ->
                    x.productNameShortcut == iOrder.productName!!.substring(iOrder.productName!!.length - 7, iOrder.productName!!.length)
                }

                val planProductCreateModel = generatePlanProductModel(product)

                val parentProcesses = processes.filter { x ->
                    !x.processStatisticCode.isNullOrEmpty() && x.processInventoryCode.isNullOrEmpty()
                        && x.processConvertCode != processSource!!.processConvertCode
                }.sortedWith(compareByDescending<ProductProcessModel> { it.dayOfImplementation ?: 0 }.thenByDescending { it.processSequence })
                val childrenProcesses = processes.filter { x ->
                    !x.processStatisticCode.isNullOrEmpty() && !x.processInventoryCode.isNullOrEmpty()
                        && x.processConvertCode != processSource!!.processConvertCode
                }.sortedWith(compareByDescending<ProductProcessModel> { it.dayOfImplementation ?: 0 }.thenByDescending { it.processSequence })

                var completionRateSource = completionRates.find { x ->
                    x.layerCode?.toIntOrNull() == processSource!!.layerCode?.toIntOrNull() && x.processCode == processSource!!.processCode
                }
                if (completionRateSource?.rate == null || completionRateSource.rate!! <= BigDecimal(0)) continue

                val planProcessINS = generatePlanProcessModel(processSource, completionRateSource.rate)
                var planDetailSource = mutableListOf(generatePlanDetailINSModel(iOrder, processSource.unit!!))
                planProcessINS.planDetails = planDetailSource
                planProductCreateModel.planProcesses.add(planProcessINS)

                var currentLayerCode = processSource.layerCode?.toIntOrNull() ?: 0
                var count = 1
                while (count <= (product.layerCount ?: 0)) {
                    val planCalculator = calculatePlanProcess(
                        iOrder.orderDate!!, processSource!!, completionRateSource!!, planDetailSource, completionRates,
                        parentProcesses.filter { it.layerCode?.toIntOrNull() == currentLayerCode },
                        childrenProcesses, product, equipmentInfo, equipmentUsedInfoByDate, holidays
                    )
                    planProductCreateModel.planProcesses.addAll(planCalculator.planProcessResults)

                    processSource = planCalculator.processSource
                    if (processSource == null) break

                    completionRateSource = planCalculator.completionRateSource
                    planDetailSource = planCalculator.planDetailSource

                    val processOfNextLayer = childrenProcesses.find { x ->
                        x.layerCode?.toIntOrNull() != currentLayerCode && x.processConvertCode == processSource!!.processConvertCode
                    }
                    if (processOfNextLayer == null) break

                    currentLayerCode = processOfNextLayer.layerCode?.toIntOrNull() ?: 0
                    count++
                }

                //region check trường hợp ghép lớp gia áp nhiệt
                val processGAN = planProductCreateModel.planProcesses.firstOrNull { x -> x.processStatisticCode == ProcessStatisticCode.GHEPLOP_GIAAPNHIET }
                if (processGAN != null) {
                    processSource = parentProcesses.first { x -> x.layerCode?.toIntOrNull() == processGAN.layerCode?.toIntOrNull() && x.processCode == processGAN.processCode }
                    completionRateSource = completionRates.first { x ->
                        x.processCode == processSource.processCode && x.layerCode?.toIntOrNull() == processSource.layerCode?.toIntOrNull()
                    }
                    planDetailSource = processGAN.planDetails
                    val processOfNextLayer = childrenProcesses.find { x ->
                        x.layerCode?.toIntOrNull() != processGAN.layerCode?.toIntOrNull() && x.processConvertCode == processSource.processConvertCode
                    }
                    if (processOfNextLayer != null) {
                        currentLayerCode = processOfNextLayer.layerCode?.toIntOrNull() ?: 0
                        val planCalculator = calculatePlanProcess(
                            iOrder.orderDate!!, processSource, completionRateSource, planDetailSource, completionRates,
                            parentProcesses.filter { it.layerCode?.toIntOrNull() == currentLayerCode },
                            childrenProcesses, product, equipmentInfo, equipmentUsedInfoByDate, holidays
                        )
                        planProductCreateModel.planProcesses.addAll(planCalculator.planProcessResults)
                    }
                }
                //endregion

                planProductCreateModel.planProcesses = mappingPlanProcessModel(planProductCreateModel.planProcesses)
                planProducts.add(planProductCreateModel)

                equipmentUsedInfoByDate = calculateEquipmentUsedInfo(planProductCreateModel.planProcesses, product, equipmentInfo, equipmentUsedInfoByDate)
            }
        }

        val planProductMappings = mappingPlanProductModel(planProducts)
        return planProductMappings
    }

    private fun calculatePlanProcess(
        orderDate: OffsetDateTime,
        processSource: ProductProcessModel,
        completionRateSource: CompletionRateProcessProduct,
        planDetailSource: MutableList<PlanDetailCreateModel>,
        completionRates: List<CompletionRateProcessProduct>,
        parentProcesses: List<ProductProcessModel>,
        childrenProcesses: List<ProductProcessModel>,
        product: Product,
        equipmentInfo: List<EquipmentProductivity>,
        equipmentUsedInfoByDate: MutableList<Pair<OffsetDateTime, EquipmentProductivity>>,
        holidays: List<OffsetDateTime>
    ): PlanCalculatorModel {
        val planProcesses = mutableListOf<PlanProcessCreateModel>()
        var currentPlanDetail = planDetailSource
        var currentCompletionRate = completionRateSource.rate
        var currentProcessUnit = processSource.unit!!
        var dayOfImplement = processSource.dayOfImplementation ?: 0
        for (iParentProcess in parentProcesses) {
            val diffDay = dayOfImplement - (iParentProcess.dayOfImplementation ?: 0)
            val completionRate = completionRates.find { x ->
                x.layerCode?.toIntOrNull() == iParentProcess.layerCode?.toIntOrNull() && x.processCode == iParentProcess.processCode
            } ?: break
            val eqConfig = equipmentInfo.find { x ->
                x.frame_1 == product.frame_1 && x.grpProcess == iParentProcess.processGroup && x.mold!!.contains(product.mold!!)
            } ?: EquipmentProductivity()

            val planProcess = generatePlanProcessModel(iParentProcess, completionRate.rate)
            currentPlanDetail = generatePlanDetailModel(
                orderDate, currentPlanDetail, currentProcessUnit, iParentProcess, diffDay,
                product, currentCompletionRate!!, eqConfig, equipmentUsedInfoByDate, holidays
            )
            planProcess.planDetails = currentPlanDetail
            planProcess.childrenProcesses = childrenProcesses.filter { x -> x.processInventoryCode == iParentProcess.processCode }
                .map { x -> generatePlanChildrenProcessModel(x) }.toMutableList()

            planProcesses.add(planProcess)

            dayOfImplement = (iParentProcess.dayOfImplementation ?: 0)
            currentProcessUnit = iParentProcess.unit!!
            currentCompletionRate = completionRate.rate
        }

        val layeringProcess = parentProcesses.filter { x ->
            x.processConvertCode!!.startsWith(ProcessConvertCode.M) && x.processStatisticCode!! != ProcessStatisticCode.GHEPLOP_GIAAPNHIET
        }.sortedByDescending { it.processSequence }.firstOrNull()

        val data = PlanCalculatorModel(
            processSource = layeringProcess,
            completionRateSource = completionRates.firstOrNull { x -> x.processCode == layeringProcess?.processCode && x.layerCode == layeringProcess?.layerCode },
            planDetailSource = planProcesses.filter { x ->
                x.processCode == layeringProcess?.processCode && x.layerCode == layeringProcess?.layerCode
            }.map { x -> x.planDetails }.flatten().toMutableList(),
            planProcessResults = planProcesses
        )
        return data
    }

    //endregion

    //region CREATE_PLAN_WITH_INVENTORY

    private fun createPlanWithInventoryHasRePlan(
        request: CreatePlanRequest,
        planCalendarConfig: PlanCalendarConfig,
        planProductData: MutableList<PlanProductCreateModel>,
        planDataTemp: PlanTempModel?,
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        hasInventory: Boolean
    ) {
        if (planDataTemp != null) {
            val planProducts = mutableListOf<PlanProductTemp>()
            val planProcesses = mutableListOf<PlanProcessTemp>()
            val planDetails = mutableListOf<PlanDetailTemp>()
            if (request.productNames.isEmpty()) {
                planDetails.addAll(planDataTemp.planDetails.filter { x -> x.planDate!!.isBefore(request.inventoryDate!!) })
                planProcesses.addAll(planDataTemp.planProcesses.filter { x -> planDetails.any { m -> m.planProcessId == x.id } })
                planProducts.addAll(planDataTemp.planProducts.filter { x -> planProcesses.any { m -> m.planProductId == x.id } })
            } else {
                var products = planDataTemp.planProducts.filter { x -> !request.productNames.any { m -> m == x.productName } }
                var processes = planDataTemp.planProcesses.filter { x -> products.any { m -> m.id == x.planProductId } }
                var details = planDataTemp.planDetails.filter { x -> products.any { m -> m.id == x.planProductId } }

                planProducts.addAll(products)
                planProcesses.addAll(processes)
                planDetails.addAll(details)

                products = planDataTemp.planProducts.filter { x -> request.productNames.any { m -> m == x.productName } }
                processes = planDataTemp.planProcesses.filter { x -> products.any { m -> m.id == x.planProductId } }
                details = planDataTemp.planDetails.filter { x -> products.any { m -> m.id == x.planProductId } && x.planDate!!.isBefore(request.inventoryDate!!) }

                planProducts.addAll(products)
                planProcesses.addAll(processes)
                planDetails.addAll(details)
            }

            val productNames = planProducts.mapNotNull { it.productName }
            val workResults = workResultRep.getForPlan(startDate, request.inventoryDate!!.plusDays(-1), productNames)

            for (item in planProductData) {
                val planProduct = planProducts.find { x -> x.productName == item.productName }
                if (planProduct == null) continue

                val wrs = workResults.filter { x -> x.itemName == item.productName }
                val parentProcesses = planProcesses.filter { x -> x.planProductId == planProduct.id && x.parentId.isNullOrEmpty() }
                val childrenProcesses = planProcesses.filter { x -> x.planProductId == planProduct.id && !x.parentId.isNullOrEmpty() }
                val details = planDetails.filter { x -> x.planProductId == planProduct.id }

                for (iProcess in parentProcesses) {
                    val iWrs = wrs.filter { it.processCode == iProcess.processCode && it.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() }
                    if (item.planProcesses.any { it.processCode == iProcess.processCode && it.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() }) {
                        item.planProcesses = item.planProcesses.map { model ->
                            if (model.processCode == iProcess.processCode && model.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()) {
                                val detailCreateModels = iWrs.groupBy { Triple(it.processCode, it.layerCode, DateTimeHelper.toString(it.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { x ->
                                    val firstValue = x.value.first()
                                    PlanDetailCreateModel(
                                        title = PlanTitle.PLAN_KEY,
                                        planDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
                                        sheetQuantity = x.value.sumOf { it.goodSheetQuantity ?: 0 },
                                        blockQuantity = x.value.sumOf { it.goodTapeQuantity ?: 0 },
                                        orderDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
                                        hasInventory = true
                                    )
                                }
//                                val detailsRemove = model.planDetails.filter { it.planDate!!.isBefore(request.inventoryDate!!) }
//                                model.planDetails.removeAll(detailsRemove)
                                model.planDetails.addAll(detailCreateModels)
                            }
                            model
                        }.toMutableList()
                    } else {
                        val processCreateModel = PlanProcessCreateModel(
                            processCode = iProcess.processCode,
                            processName = iProcess.processName,
                            processConvertCode = iProcess.processConvertCode,
                            layerCode = iProcess.layerCode,
                            completionRate = iProcess.completionRate,
                            inventory = iProcess.inventory,
                            unit = iProcess.unit,
                            processSequence = iProcess.processSequence,
                            processNameJp = iProcess.processNameJp,
                            processGroup = iProcess.processGroup,
                            processStatisticCode = iProcess.processStatisticCode,
                            childrenProcesses = childrenProcesses.filter { it.parentId == iProcess.id }.map { x ->
                                PlanChildrenProcessCreateModel(
                                    processCode = x.processCode,
                                    processName = x.processName,
                                    processConvertCode = x.processConvertCode,
                                    layerCode = x.layerCode,
                                    completionRate = x.completionRate,
                                    inventory = x.inventory,
                                    unit = x.unit,
                                    processSequence = x.processSequence,
                                    processNameJp = x.processNameJp,
                                    processGroup = x.processGroup,
                                    processStatisticCode = x.processStatisticCode
                                )
                            }.toMutableList(),
                            planDetails = iWrs.groupBy { Triple(it.processCode, it.layerCode, DateTimeHelper.toString(it.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { x ->
                                val firstValue = x.value.first()
                                PlanDetailCreateModel(
                                    title = PlanTitle.PLAN_KEY,
                                    planDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
                                    sheetQuantity = x.value.sumOf { it.goodSheetQuantity ?: 0 },
                                    blockQuantity = x.value.sumOf { it.goodTapeQuantity ?: 0 },
                                    orderDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
                                    hasInventory = true
                                )
                            }.toMutableList()
                        )
                        item.planProcesses.add(processCreateModel)
                    }
                }
            }

            for (iPlanProduct in planProducts.filter { x -> !planProductData.any { it.productName == x.productName } }) {
                //val wrs = workResults.filter { x -> x.itemName == iPlanProduct.productName }
                val processes = planProcesses.filter { x -> x.planProductId == iPlanProduct.id }
                val details = planDetails.filter { x -> x.planProductId == iPlanProduct.id }
                planProductData.add(generatePlanProductModel(iPlanProduct, processes, details))
            }
        }

        planRep.createPlanTemp(generatePlanModel(request, planCalendarConfig, startDate, endDate, hasInventory), planProductData)
    }

    private fun createPlanWithInventoryNoRePlan(
        request: CreatePlanRequest,
        planCalendarConfig: PlanCalendarConfig,
        planProductData: MutableList<PlanProductCreateModel>,
        planDataTemp: PlanTempModel?,
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        hasInventory: Boolean
    ) {
        if (planDataTemp != null) {
            val planProducts = mutableListOf<PlanProductTemp>()
            val planProcesses = mutableListOf<PlanProcessTemp>()
            val planDetails = mutableListOf<PlanDetailTemp>()
            if (request.productNames.isEmpty()) {
                planDetails.addAll(planDataTemp.planDetails.filter { x -> x.planDate!!.isBefore(request.inventoryDate!!) })
                planProcesses.addAll(planDataTemp.planProcesses.filter { x -> planDetails.any { m -> m.planProcessId == x.id } })
                planProducts.addAll(planDataTemp.planProducts.filter { x -> planProcesses.any { m -> m.planProductId == x.id } })
            } else {
                var products = planDataTemp.planProducts.filter { x -> !request.productNames.any { m -> m == x.productName } }
                var processes = planDataTemp.planProcesses.filter { x -> products.any { m -> m.id == x.planProductId } }
                var details = planDataTemp.planDetails.filter { x -> products.any { m -> m.id == x.planProductId } }

                planProducts.addAll(products)
                planProcesses.addAll(processes)
                planDetails.addAll(details)

                products = planDataTemp.planProducts.filter { x -> request.productNames.any { m -> m == x.productName } }
                processes = planDataTemp.planProcesses.filter { x -> products.any { m -> m.id == x.planProductId } }
                details = planDataTemp.planDetails.filter { x -> products.any { m -> m.id == x.planProductId } && x.planDate!!.isBefore(request.inventoryDate!!) }

                planProducts.addAll(products)
                planProcesses.addAll(processes)
                planDetails.addAll(details)
            }

            for (item in planProductData) {
                val planProduct = planProducts.find { x -> x.productName == item.productName }
                if (planProduct == null) continue

                val parentProcesses = planProcesses.filter { x -> x.planProductId == planProduct.id && x.parentId.isNullOrEmpty() }
                val childrenProcesses = planProcesses.filter { x -> x.planProductId == planProduct.id && !x.parentId.isNullOrEmpty() }
                val details = planDetails.filter { x -> x.planProductId == planProduct.id }

                for (iProcess in parentProcesses) {
                    if (item.planProcesses.any { it.processCode == iProcess.processCode && it.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() }) {
                        item.planProcesses = item.planProcesses.map { model ->
                            if (model.processCode == iProcess.processCode && model.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()) {
                                val detailCreateModels = details.filter { it.planProcessId == iProcess.id }.map { x ->
                                    PlanDetailCreateModel(
                                        title = x.title,
                                        planDate = x.planDate,
                                        sheetQuantity = x.sheetQuantity,
                                        blockQuantity = x.blockQuantity,
                                        orderDate = x.orderDate,
                                        hasInventory = x.hasInventory
                                    )
                                }
                                model.planDetails.addAll(detailCreateModels)
                            }
                            model
                        }.toMutableList()
                    } else {
                        val processCreateModel = PlanProcessCreateModel(
                            processCode = iProcess.processCode,
                            processName = iProcess.processName,
                            processConvertCode = iProcess.processConvertCode,
                            layerCode = iProcess.layerCode,
                            completionRate = iProcess.completionRate,
                            inventory = iProcess.inventory,
                            unit = iProcess.unit,
                            processSequence = iProcess.processSequence,
                            processNameJp = iProcess.processNameJp,
                            processGroup = iProcess.processGroup,
                            processStatisticCode = iProcess.processStatisticCode,
                            childrenProcesses = childrenProcesses.filter { it.parentId == iProcess.id }.map { x ->
                                PlanChildrenProcessCreateModel(
                                    processCode = x.processCode,
                                    processName = x.processName,
                                    processConvertCode = x.processConvertCode,
                                    layerCode = x.layerCode,
                                    completionRate = x.completionRate,
                                    inventory = x.inventory,
                                    unit = x.unit,
                                    processSequence = x.processSequence,
                                    processNameJp = x.processNameJp,
                                    processGroup = x.processGroup,
                                    processStatisticCode = x.processStatisticCode
                                )
                            }.toMutableList(),
                            planDetails = details.filter { it.planProcessId == iProcess.id }.map { x ->
                                PlanDetailCreateModel(
                                    title = x.title,
                                    planDate = x.planDate,
                                    sheetQuantity = x.sheetQuantity,
                                    blockQuantity = x.blockQuantity,
                                    orderDate = x.orderDate,
                                    hasInventory = x.hasInventory
                                )
                            }.toMutableList()
                        )
                        item.planProcesses.add(processCreateModel)
                    }
                }
            }

            for (iPlanProduct in planProducts.filter { x -> !planProductData.any { it.productName == x.productName } }) {
                val processes = planProcesses.filter { x -> x.planProductId == iPlanProduct.id }
                val details = planDetails.filter { x -> x.planProductId == iPlanProduct.id }
                planProductData.add(generatePlanProductModel(iPlanProduct, processes, details))
            }
        }

        planRep.createPlanTemp(generatePlanModel(request, planCalendarConfig, startDate, endDate, hasInventory), planProductData)
    }

    private fun calculatePlanProductWithInventory(
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        productProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        equipmentInfo: List<EquipmentProductivity>,
        inventories: List<InventoryProductResponse>,
        holidays: List<OffsetDateTime>
    ): MutableList<PlanProductCreateModel> {
        val planProducts = mutableListOf<PlanProductCreateModel>()
        val productNames = orderInfo.mapNotNull { x -> x.productName }.distinct()
        val parentProcesses = productProcesses.filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && x.processInventoryCode.isNullOrEmpty()
                && x.processConvertCode != ProcessConvertCode.INS
        }.sortedWith(compareByDescending<ProductProcessModel> { it.dayOfImplementation ?: 0 }.thenByDescending { it.processSequence })
        val childrenProcesses = productProcesses.filter { x ->
            !x.processStatisticCode.isNullOrEmpty() && !x.processInventoryCode.isNullOrEmpty()
                && x.processConvertCode != ProcessConvertCode.INS
        }.sortedWith(compareByDescending<ProductProcessModel> { it.dayOfImplementation ?: 0 }.thenByDescending { it.processSequence })

        val mAllParents = parentProcesses.filter { x -> x.processConvertCode == ProcessConvertCode.M_ALL }
        val mAllChild = childrenProcesses.filter { x -> x.processConvertCode == ProcessConvertCode.M_ALL }

        for (iProductName in productNames) {
            val processes = productProcesses.filter { x -> x.productName == iProductName }
            val processIns = processes.firstOrNull { x -> x.processConvertCode == ProcessConvertCode.INS } ?: continue
            val mAllParent = mAllParents.firstOrNull { it.productName == iProductName } ?: continue
            val mAllChildren = mAllChild.firstOrNull { it.productName == iProductName } ?: continue
            val orders = orderInfo.filter { x -> x.productName == iProductName }
            val inventoryByProducts = inventories.filter { x -> x.productName == iProductName }
            var orderAllocations = allocateInventoryIns(orders, inventoryByProducts, processIns).filter { (it.quantity ?: 0) > 0 }
            if (orderAllocations.isEmpty()) continue

            val product = productInfo.firstOrNull { x -> x.name == iProductName }
            if (product == null) continue

            val planProductCreateModel = generatePlanProductModel(product)

            val completionRates = completionRateInfo.filter { x -> x.productNameShortcut == iProductName.substring(iProductName.length - 7, iProductName.length) }

            val completionRateIns = completionRates.firstOrNull { x -> x.processCode == ProcessCode.INS }
            val planProcessINS = generatePlanProcessModel(processIns, completionRateIns!!.rate)
            for (iOrder in orders) {
                val planDetailIns = generatePlanDetailINSModel(iOrder, processIns.unit!!)
                planProcessINS.planDetails.add(planDetailIns)
            }
            planProductCreateModel.planProcesses.add(planProcessINS)

            if (inventories.isNotEmpty()) {
                val highestPriorityProcesses = parentProcesses.filter { x ->
                    x.productName == iProductName && x.layerCode?.toIntOrNull() == mAllParent.layerCode?.toIntOrNull()
                        && x.processSequence!! >= mAllParent.processSequence!! && x.processConvertCode != ProcessConvertCode.INS
                }.sortedBy { x -> x.processSequence }

                val planHighestPriority = createPlanFromInventoryHighestPriority(
                    processIns, highestPriorityProcesses, childrenProcesses.filter { x -> x.productName == iProductName },
                    orderAllocations, product, inventoryByProducts, completionRates, holidays
                )
                planProductCreateModel.planProcesses.addAll(planHighestPriority.second)

                orderAllocations = planHighestPriority.first.filter { (it.quantity ?: 0) > 0 }
                if (orderAllocations.isEmpty()) {
                    planProducts.add(planProductCreateModel)
                    continue
                }

                val secondPriorityProcesses = listOf(
                    parentProcesses.filter { x ->
                        x.productName == iProductName && x.layerCode?.toIntOrNull() == mAllParent.layerCode?.toIntOrNull()
                            && x.processSequence!! <= mAllParent.processSequence!! - 1
                    }.sortedByDescending { it.processSequence }.first(),
                    parentProcesses.filter { x ->
                        x.productName == iProductName && x.layerCode?.toIntOrNull() == mAllChildren.layerCode?.toIntOrNull()
                            && x.processSequence!! <= mAllChildren.processSequence!! - 1
                    }.sortedByDescending { it.processSequence }.first()
                )
                val planSecondPriority = createPlanFromInventorySecondPriority(
                    processIns, mAllParent, secondPriorityProcesses,
                    parentProcesses.filter { x -> x.productName == iProductName },
                    childrenProcesses.filter { x -> x.productName == iProductName },
                    orderAllocations, product, inventoryByProducts, completionRates, holidays
                )
                planProductCreateModel.planProcesses.addAll(planSecondPriority.second)

                orderAllocations = planSecondPriority.first.filter { (it.quantity ?: 0) > 0 }
                if (orderAllocations.isEmpty()) {
                    planProducts.add(planProductCreateModel)
                    continue
                }

                val lowestPriorityProcesses = parentProcesses.filter { x ->
                    x.productName == iProductName
                        && (
                        (x.layerCode?.toIntOrNull() == mAllParent.layerCode?.toIntOrNull() && x.processSequence!! < mAllParent.processSequence!! - 1)
                            || (x.layerCode?.toIntOrNull() == mAllChildren.layerCode?.toIntOrNull() && x.processSequence!! < mAllChildren.processSequence!! - 1)
                            || (x.layerCode?.toIntOrNull() != mAllParent.layerCode?.toIntOrNull() && x.layerCode?.toIntOrNull() != mAllChildren.layerCode?.toIntOrNull())
                        )
                }
                val planLowestPriority = createPlanFromInventoryLowestPriority(
                    processIns, lowestPriorityProcesses, processes, orderAllocations,
                    product, inventoryByProducts, completionRates, holidays
                )
                planProductCreateModel.planProcesses.addAll(planLowestPriority.second)

                orderAllocations = planLowestPriority.first.filter { (it.quantity ?: 0) > 0 }
                if (orderAllocations.isEmpty()) {
                    planProducts.add(planProductCreateModel)
                    continue
                }
            }

            val planAfterAllocate = calculatePlanProcessAfterAllocate(
                processIns, orderAllocations,
                parentProcesses.filter { x -> x.productName == iProductName },
                childrenProcesses.filter { x -> x.productName == iProductName },
                completionRates, product, holidays
            )
            planProductCreateModel.planProcesses.addAll(planAfterAllocate)

            planProducts.add(planProductCreateModel)
        }

        val planProductMappings = mappingPlanProductModel(planProducts)

        return planProductMappings
    }

    private fun allocateInventoryIns(orderInfo: List<OrderInfo>, inventories: List<InventoryProductResponse>, processIns: ProductProcessModel): List<OrderInfo> {
        val inventoryIns = inventories.firstOrNull { x -> x.processCode == ProcessCode.INS }
        val orderInfoAllocation = mutableListOf<OrderInfo>()
        if (inventoryIns == null) {
            orderInfoAllocation.addAll(orderInfo)
            return orderInfoAllocation
        }
        var quantity = if (processIns.unit == ProcessUnit.SHEET) inventoryIns.sheetQuantity ?: 0 else inventoryIns.productQuantity ?: 0
        if (quantity <= 0) {
            orderInfoAllocation.addAll(orderInfo)
            return orderInfoAllocation
        }
        val orderClones = orderInfo.map { item ->
            OrderInfo(
                id = item.id,
                productName = item.productName,
                frame_1 = item.frame_1,
                layerCount = item.layerCount,
                pcsSh = item.pcsSh,
                blockSh = item.blockSh,
                srNosr = item.srNosr,
                version = item.version,
                orderDate = item.orderDate,
                quantity = item.quantity,
                isLatest = item.isLatest,
                isChangeQuantity = item.isChangeQuantity
            )
        }.sortedBy { it.orderDate }
        for (order in orderClones) {
            if (order.quantity!! > quantity) {
                order.quantity = order.quantity!! - quantity
                quantity = 0
            } else {
                quantity -= order.quantity!!
                order.quantity = 0

            }
            if (order.quantity!! > 0) orderInfoAllocation.add(order)
        }

        return orderInfoAllocation
    }

    private fun allocateInventoryInsFromProcess(orderInfo: List<OrderInfo>, inventoryIns: Int): Pair<List<AllocationRateByDateModel>, List<OrderInfo>> {
        val orderInfoAllocation = mutableListOf<OrderInfo>()
        val allocationRates = mutableListOf<AllocationRateByDateModel>()
        var quantity = inventoryIns
        var count = 1
        val orderClones = orderInfo.map { item ->
            OrderInfo(
                id = item.id,
                productName = item.productName,
                frame_1 = item.frame_1,
                layerCount = item.layerCount,
                pcsSh = item.pcsSh,
                blockSh = item.blockSh,
                srNosr = item.srNosr,
                version = item.version,
                orderDate = item.orderDate,
                quantity = item.quantity,
                isLatest = item.isLatest,
                isChangeQuantity = item.isChangeQuantity
            )
        }.sortedBy { it.orderDate }
        for (order in orderClones) {
            var quantityUsed: Int
            if (count == orderClones.size) {
                if (order.quantity!! > quantity) {
                    order.quantity = order.quantity!! - quantity
                    quantityUsed = quantity
                    quantity = 0
                } else {
                    quantityUsed = quantity
                    order.quantity = 0
                    quantity = 0
                }
                if (order.quantity!! > 0) orderInfoAllocation.add(order)
                if (quantityUsed > 0) {
                    val rate = NumberHelper.toDecimal(quantityUsed * 100) / NumberHelper.toDecimal(inventoryIns)
                    allocationRates.add(AllocationRateByDateModel(order.orderDate!!, quantityUsed, rate))
                }
            } else {
                if (order.quantity!! > quantity) {
                    order.quantity = order.quantity!! - quantity
                    quantityUsed = quantity
                    quantity = 0
                } else {
                    quantityUsed = order.quantity!!
                    quantity -= order.quantity!!
                    order.quantity = 0
                }
                if (order.quantity!! > 0) orderInfoAllocation.add(order)
                if (quantityUsed > 0) {
                    val rate = NumberHelper.toDecimal(quantityUsed * 100) / NumberHelper.toDecimal(inventoryIns)
                    allocationRates.add(AllocationRateByDateModel(order.orderDate!!, quantityUsed, rate))
                }
            }
            count++
        }

        return Pair(allocationRates, orderInfoAllocation)
    }

    private fun createPlanFromInventoryHighestPriority(
        processIns: ProductProcessModel,
        processes: List<ProductProcessModel>,
        childrenProcesses: List<ProductProcessModel>,
        orderInfo: List<OrderInfo>,
        productInfo: Product,
        inventories: List<InventoryProductResponse>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        holidays: List<OffsetDateTime>
    ): Pair<List<OrderInfo>, List<PlanProcessCreateModel>> {
        val inventoriesByProcess = inventories.filter { x ->
            x.productName == productInfo.name
                && processes.any { m ->
                m.productName == x.productName && m.processCode == x.processCode
                    && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
            }
        }
        var orderAllocations = orderInfo
        val completionRateIns = completionRateInfo.firstOrNull { x -> x.processCode == ProcessCode.INS }?.rate ?: return Pair(orderInfo, listOf())
        val dataCalculatePlanFromInventory = mutableListOf<Pair<List<Triple<String?, BigDecimal, Int>>, List<AllocationRateByDateModel>>>()

        for (iProcess in processes.sortedByDescending { it.processSequence }) {
            if (orderAllocations.isEmpty()) break
            val childrenProcess = childrenProcesses.filter { x -> x.processInventoryCode == iProcess.processCode }
            val inventoriesByChildrenProcess = inventories.filter { x ->
                x.productName == productInfo.name
                    && childrenProcess.any { m ->
                    m.productName == x.productName && m.processCode == x.processCode
                        && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
                }
            }
            val inventoryProcess = inventoriesByProcess.filter { x ->
                x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
            }
            if (inventoryProcess.isEmpty() && inventoriesByChildrenProcess.isEmpty()) continue

            val sheetInventory = BigDecimal(inventoryProcess.sumOf { x -> x.sheetQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.sheetQuantity ?: 0 })
            val blockInventory = BigDecimal(inventoryProcess.sumOf { x -> x.productQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.productQuantity ?: 0 })

            var currentInventory = if (iProcess.unit == ProcessUnit.SHEET) {
                if (sheetInventory == BigDecimal(0)) {
                    blockInventory / BigDecimal(productInfo.shBlock!!)
                } else {
                    sheetInventory
                }
            } else {
                if (blockInventory == BigDecimal(0)) {
                    sheetInventory * BigDecimal(productInfo.shBlock!!)
                } else {
                    blockInventory
                }
            }

            val processCalculateFromInventories = mutableListOf(Triple(iProcess.processCode, currentInventory, iProcess.layerCode!!.toInt()))
            var currentProcessUnit = iProcess.unit
            for (prc in processes.filter { x -> x.processSequence!! > iProcess.processSequence!! }.sortedBy { it.processSequence }) {
                val completionRate = completionRateInfo.firstOrNull { x ->
                    x.processCode == prc.processCode && x.layerCode?.toIntOrNull() == prc.layerCode?.toIntOrNull()
                }?.rate ?: break

                currentInventory = if (currentProcessUnit == ProcessUnit.SHEET) {
                    if (prc.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate * BigDecimal(productInfo.shBlock!!)) / BigDecimal(100)
                    }
                } else {
                    if (prc.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate) / (BigDecimal(productInfo.shBlock!!) * BigDecimal(100))
                    }
                }

                processCalculateFromInventories.add(Triple(prc.processCode, currentInventory, prc.layerCode!!.toInt()))
                currentProcessUnit = prc.unit
            }

            val inventoryIns = NumberHelper.roundedUp((currentInventory * completionRateIns) / BigDecimal(100))
            val allocateInventoryIns = allocateInventoryInsFromProcess(orderAllocations, inventoryIns)

            orderAllocations = allocateInventoryIns.second

            dataCalculatePlanFromInventory.add(Pair(processCalculateFromInventories, allocateInventoryIns.first))
        }

        val processCreateModels = mutableListOf<PlanProcessCreateModel>()
        for (item in dataCalculatePlanFromInventory) {
            val lstProcess = processes.filter { x -> item.first.any { m -> m.first == x.processCode } }
            val dataPlanProcessCreateModel = createPlanProcess(
                processIns, item.second, lstProcess, childrenProcesses,
                completionRateInfo, productInfo, item.first, holidays
            )
            processCreateModels.addAll(dataPlanProcessCreateModel)
        }

        return Pair(orderAllocations, processCreateModels)
    }

    private fun createPlanFromInventorySecondPriority(
        processIns: ProductProcessModel,
        mAllParent: ProductProcessModel,
        processesCalculator: List<ProductProcessModel>,
        parentProcesses: List<ProductProcessModel>,
        childrenProcesses: List<ProductProcessModel>,
        orderInfo: List<OrderInfo>,
        productInfo: Product,
        inventories: List<InventoryProductResponse>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        holidays: List<OffsetDateTime>
    ): Pair<List<OrderInfo>, List<PlanProcessCreateModel>> {
        val inventoriesByProcess = inventories.filter { x ->
            x.productName == productInfo.name
                && processesCalculator.any { m ->
                m.productName == x.productName && m.processCode == x.processCode
                    && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
            }
        }
        val lstInventory = mutableListOf<Triple<ProductProcessModel, BigDecimal, BigDecimal>>()
        for (iProcess in processesCalculator) {
            val childrenProcess = childrenProcesses.filter { x -> x.processInventoryCode == iProcess.processCode }
            val inventoriesByChildrenProcess = inventories.filter { x ->
                x.productName == productInfo.name
                    && childrenProcess.any { m ->
                    m.productName == x.productName && m.processCode == x.processCode
                        && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
                }
            }
            val inventoryProcess = inventoriesByProcess.filter { x -> x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() }
            if (inventoryProcess.isEmpty() && inventoriesByChildrenProcess.isEmpty()) return Pair(orderInfo, listOf())

            var sheetInventory: BigDecimal
            var blockInventory: BigDecimal

            if (iProcess.unit == ProcessUnit.SHEET) {
                sheetInventory = BigDecimal(inventoryProcess.sumOf { x -> x.sheetQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.sheetQuantity ?: 0 })
                blockInventory = sheetInventory * BigDecimal(productInfo.shBlock!!)
            } else {
                blockInventory = BigDecimal(inventoryProcess.sumOf { x -> x.productQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.productQuantity ?: 0 })
                sheetInventory = blockInventory / BigDecimal(productInfo.shBlock!!)
            }

            if (sheetInventory <= BigDecimal(0) || blockInventory <= BigDecimal(0)) return Pair(orderInfo, listOf())

            lstInventory.add(Triple(iProcess, sheetInventory, blockInventory))
        }
        val source = lstInventory.minBy { it.second }
        val processSource = source.first
        var currentInventory = if (processSource.unit == ProcessUnit.SHEET) source.second else source.third

        val source2 = lstInventory.maxBy { it.second }
        val processSource2 = source2.first

        val processCalculateFromInventories = mutableListOf(
            Triple(processSource.processCode, currentInventory, processSource.layerCode!!.toInt()),
            Triple(processSource2.processCode, if (processSource2.unit == ProcessUnit.SHEET) source2.second else source2.third, processSource2.layerCode!!.toInt())
        )

        var orderAllocations = orderInfo
        val completionRateIns = completionRateInfo.firstOrNull { x -> x.processCode == ProcessCode.INS }?.rate ?: return Pair(orderInfo, listOf())

        val processes = parentProcesses.filter { x ->
            x.productName == productInfo.name && x.layerCode?.toIntOrNull() == mAllParent.layerCode?.toIntOrNull()
                && x.processSequence!! >= mAllParent.processSequence!! && x.processConvertCode != ProcessConvertCode.INS
        }.sortedBy { it.processSequence }.toMutableList()

        var currentProcessUnit = processSource.unit
        for (iProcess in processes) {
            val completionRate = completionRateInfo.firstOrNull { x ->
                x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
            }?.rate ?: break

            currentInventory = if (currentProcessUnit == ProcessUnit.SHEET) {
                if (iProcess.unit == currentProcessUnit) {
                    (currentInventory * completionRate) / BigDecimal(100)
                } else {
                    (currentInventory * completionRate * BigDecimal(productInfo.shBlock!!)) / BigDecimal(100)
                }
            } else {
                if (iProcess.unit == currentProcessUnit) {
                    (currentInventory * completionRate) / BigDecimal(100)
                } else {
                    (currentInventory * completionRate) / (BigDecimal(productInfo.shBlock!!) * BigDecimal(100))
                }
            }

            processCalculateFromInventories.add(Triple(iProcess.processCode, currentInventory, iProcess.layerCode!!.toInt()))
            currentProcessUnit = iProcess.unit
        }

        val inventoryIns = NumberHelper.roundedUp((currentInventory * completionRateIns) / BigDecimal(100))
        val allocateInventoryIns = allocateInventoryInsFromProcess(orderAllocations, inventoryIns)
        orderAllocations = allocateInventoryIns.second

        processes.addAll(processesCalculator)
        val processCreateModels = createPlanProcess(
            processIns, allocateInventoryIns.first, processes, childrenProcesses,
            completionRateInfo, productInfo, processCalculateFromInventories, holidays
        )

        return Pair(orderAllocations, processCreateModels)
    }

    private fun createPlanFromInventoryLowestPriority(
        processIns: ProductProcessModel,
        processesCalculator: List<ProductProcessModel>,
        processes: List<ProductProcessModel>,
        orderInfo: List<OrderInfo>,
        productInfo: Product,
        inventories: List<InventoryProductResponse>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        holidays: List<OffsetDateTime>
    ): Pair<List<OrderInfo>, List<PlanProcessCreateModel>> {
        val inventoriesByProcess = mutableListOf<InventoryProductResponse>()

        for (iProcess in processesCalculator) {
            val inventoryParentProcess = inventories.filter { x ->
                x.productName == productInfo.name && x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
            }
            val childrenProcess = processes.filter { it.processInventoryCode == iProcess.processCode }
            val inventoryChildrenProcess = inventories.filter { x ->
                x.productName == productInfo.name
                    && childrenProcess.any { m ->
                    m.productName == x.productName && m.processCode == x.processCode
                        && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
                }
            }
            if (inventoryParentProcess.isEmpty() && inventoryChildrenProcess.isEmpty()) continue
            inventoriesByProcess.add(
                InventoryProductResponse(
                    inventoryDate = inventoryParentProcess.firstOrNull()?.inventoryDate ?: inventoryChildrenProcess.firstOrNull()?.inventoryDate,
                    productQuantity = inventoryParentProcess.sumOf { it.productQuantity ?: 0 } + inventoryChildrenProcess.sumOf { it.productQuantity ?: 0 },
                    sheetQuantity = inventoryParentProcess.sumOf { it.sheetQuantity ?: 0 } + inventoryChildrenProcess.sumOf { it.sheetQuantity ?: 0 },
                    productName = productInfo.name,
                    processName = iProcess.processName,
                    processCode = iProcess.processCode,
                    layerCode = iProcess.layerCode
                )
            )
        }

        if (inventoriesByProcess.isEmpty()) return Pair(orderInfo, listOf())

        var orderAllocations = orderInfo
        val completionRateIns = completionRateInfo.firstOrNull { x -> x.processCode == ProcessCode.INS }?.rate ?: return Pair(orderInfo, listOf())
        val processGroupBy = processes.groupBy { x -> x.layerCode }
        val dt = processGroupBy.filter { x ->
            x.value.count { m -> m.processConvertCode!!.startsWith(ProcessConvertCode.M) && m.processInventoryCode.isNullOrEmpty() } > 1
        }.values

        if (dt.isNotEmpty()) {
            val layerProcessAbnormal = dt.first().sortedBy { it.processSequence }

            val layeringProcess = layerProcessAbnormal.filter { x ->
                x.processConvertCode!!.startsWith(ProcessConvertCode.M) && x.processInventoryCode.isNullOrEmpty()
            }.map { x -> Pair(x.processSequence!!, x.processConvertCode) }.sortedBy { it.first }

            val processCalculateFromInventories = mutableListOf<Triple<String, BigDecimal, Int>>()
            val preLayeringProcess = mutableListOf<Triple<String, String, BigDecimal>>()

            for (iLayeringProcess in layeringProcess) {
                val lstProcess = processGroupBy.filter { x ->
                    x.value.any { m -> !m.processInventoryCode.isNullOrEmpty() && m.processConvertCode == iLayeringProcess.second }
                }.values.first().sortedBy { it.processSequence }

                val data = calculateInventoryInLayerLowestPriority(
                    lstProcess.filter { x -> x.processInventoryCode.isNullOrEmpty() },
                    inventoriesByProcess, completionRateInfo, productInfo
                ).toMutableList()
                processCalculateFromInventories.addAll(data)

                val layering = lstProcess.last().processConvertCode!!
                val preLayeringProcessCode = lstProcess.filter { x ->
                    x.processInventoryCode.isNullOrEmpty() && x.processSequence!! <= iLayeringProcess.first - 1
                }.sortedByDescending { it.processSequence }.first().processCode
                val preLayering = data.first { x -> x.first == preLayeringProcessCode }
                preLayeringProcess.add(Triple(layering, preLayering.first, preLayering.second))
            }

            var currentInventory = BigDecimal(0)
            var currentProcessUnit = layerProcessAbnormal.first { x -> x.processInventoryCode.isNullOrEmpty() }.unit

            for (iProcess in layerProcessAbnormal.filter { x -> x.processInventoryCode.isNullOrEmpty() }) {
                val completionRate = completionRateInfo.firstOrNull { x ->
                    x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
                }?.rate ?: break

                val inventoryProcess = inventories.filter { x -> x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() }
                val childrenProcess = layerProcessAbnormal.filter { x -> !x.processInventoryCode.isNullOrEmpty() && x.processInventoryCode == iProcess.processCode }
                val inventoriesByChildrenProcess = inventories.filter { x ->
                    childrenProcess.any { m ->
                        m.productName == x.productName && m.processCode == x.processCode
                            && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull()
                    }
                }
                val inventory = if (iProcess.unit == ProcessUnit.SHEET) {
                    inventoryProcess.sumOf { x -> x.sheetQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.sheetQuantity ?: 0 }
                } else {
                    inventoryProcess.sumOf { x -> x.productQuantity ?: 0 } + inventoriesByChildrenProcess.sumOf { x -> x.productQuantity ?: 0 }
                }

                if (inventory == 0 && currentInventory == BigDecimal(0)) continue

                if (currentInventory == BigDecimal(0)) {
                    currentInventory = BigDecimal(inventory)
                    currentProcessUnit = iProcess.unit
                }

                val iPreLayeringProcess = preLayeringProcess.find { x -> x.first == iProcess.processConvertCode }
                if (iPreLayeringProcess != null) {
                    currentInventory = BigDecimal(min(currentInventory.toDouble(), iPreLayeringProcess.third.toDouble()))
                }

                currentInventory = if (currentProcessUnit == ProcessUnit.SHEET) {
                    if (iProcess.unit == currentProcessUnit) {
                        currentInventory * completionRate
                    } else {
                        currentInventory * completionRate * BigDecimal(productInfo.shBlock!!)
                    }
                } else {
                    if (iProcess.unit == currentProcessUnit) {
                        currentInventory * completionRate
                    } else {
                        (currentInventory * completionRate) / BigDecimal(productInfo.shBlock!!)
                    }
                }
                currentInventory += BigDecimal(inventory)

                processCalculateFromInventories.add(Triple(iProcess.processCode!!, currentInventory, iProcess.layerCode!!.toInt()))
                currentProcessUnit = iProcess.unit
            }

            val inventoryIns = NumberHelper.roundedUp(currentInventory * completionRateIns)
            val allocateInventoryIns = allocateInventoryInsFromProcess(orderAllocations, inventoryIns)

            orderAllocations = allocateInventoryIns.second

            val processCreateModels = createPlanProcess(
                processIns, allocateInventoryIns.first,
                processes.filter { x -> x.processInventoryCode.isNullOrEmpty() && x.processConvertCode != ProcessConvertCode.INS },
                processes.filter { x -> !x.processInventoryCode.isNullOrEmpty() && x.processConvertCode != ProcessConvertCode.INS },
                completionRateInfo, productInfo, processCalculateFromInventories, holidays
            )

            return Pair(orderAllocations, processCreateModels)
        } else {
            val firstLayering = processGroupBy.filter { x ->
                x.value.count { m -> m.processConvertCode!!.startsWith(ProcessConvertCode.M) } == 1
                    && !x.value.any { m -> m.processConvertCode == ProcessConvertCode.INS }
            }.values.first().sortedBy { it.processSequence }

            val processCalculateFromInventories = calculateInventoryInLayerLowestPriority(
                firstLayering.filter { x -> x.processInventoryCode.isNullOrEmpty() },
                inventoriesByProcess, completionRateInfo, productInfo
            ).toMutableList()

            var layeringProcess = firstLayering.last()
            var preLayeringProcessCode = firstLayering.filter { x ->
                x.processInventoryCode.isNullOrEmpty() && x.processSequence!! <= layeringProcess.processSequence!! - 1
            }.sortedByDescending { it.processSequence }.first().processCode
            var preLayeringProcess = processCalculateFromInventories.first { x -> x.first == preLayeringProcessCode }

            var count = 2
            var layerCode = firstLayering.first().layerCode
            while (count <= processGroupBy.size) {
                val nextLayering = processGroupBy.filter { x ->
                    x.key != firstLayering.first().layerCode
                        && x.value.any { m -> m.processConvertCode == layeringProcess.processConvertCode }
                }.values.first().sortedBy { it.processSequence }

                val data = calculateInventoryInLayerLowestPriority(
                    nextLayering.filter { x -> x.processInventoryCode.isNullOrEmpty() },
                    inventoriesByProcess, completionRateInfo, productInfo, layeringProcess, preLayeringProcess
                )

                processCalculateFromInventories.addAll(data)

                layeringProcess = nextLayering.last()
                layerCode = nextLayering.first().layerCode
                preLayeringProcessCode = nextLayering.filter { x ->
                    x.processInventoryCode.isNullOrEmpty() && x.processSequence!! <= layeringProcess.processSequence!! - 1
                }.sortedByDescending { it.processSequence }.first().processCode
                preLayeringProcess = data.firstOrNull { x -> x.first == preLayeringProcessCode } ?: break

                count++
            }

            val currentInventory = processCalculateFromInventories.first { x ->
                x.first == preLayeringProcessCode && x.third == layerCode!!.toInt()
            }.second
            val inventoryIns = NumberHelper.roundedUp((currentInventory * completionRateIns) / BigDecimal(100))
            val allocateInventoryIns = allocateInventoryInsFromProcess(orderAllocations, inventoryIns)

            orderAllocations = allocateInventoryIns.second

            val processCreateModels = createPlanProcess(
                processIns, allocateInventoryIns.first,
                processes.filter { x -> x.processInventoryCode.isNullOrEmpty() && x.processConvertCode != ProcessConvertCode.INS },
                processes.filter { x -> !x.processInventoryCode.isNullOrEmpty() && x.processConvertCode != ProcessConvertCode.INS },
                completionRateInfo, productInfo, processCalculateFromInventories, holidays
            )

            return Pair(orderAllocations, processCreateModels)
        }
    }

    private fun calculateInventoryInLayerLowestPriority(
        parentProcesses: List<ProductProcessModel>,
        inventories: List<InventoryProductResponse>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        productInfo: Product,
        layeringProcess: ProductProcessModel? = null,
        preLayeringProcess: Triple<String, BigDecimal, Int>? = null
    ): List<Triple<String, BigDecimal, Int>> {
        val data = mutableListOf<Triple<String, BigDecimal, Int>>()

        for (inv in inventories) {
            val process = parentProcesses.firstOrNull { it.processCode == inv.processCode && it.layerCode?.toIntOrNull() == inv.layerCode?.toIntOrNull() }
            if (process == null) continue
            var currentInventory = BigDecimal(if (process.unit == ProcessUnit.SHEET) inv.sheetQuantity!! else inv.productQuantity!!)
            var currentProcessUnit = process.unit

            data.add(Triple(inv.processCode!!, currentInventory, inv.layerCode!!.toInt()))

            for (iProcess in parentProcesses.filter { it.processSequence!! > process.processSequence!! }.sortedBy { it.processSequence }) {
                val completionRate = completionRateInfo.firstOrNull { x ->
                    x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
                }?.rate ?: break

                currentInventory = if (currentProcessUnit == ProcessUnit.SHEET) {
                    if (iProcess.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate * BigDecimal(productInfo.shBlock!!)) / BigDecimal(100)
                    }
                } else {
                    if (iProcess.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate) / (BigDecimal(productInfo.shBlock!!) * BigDecimal(100))
                    }
                }

                data.add(Triple(iProcess.processCode!!, currentInventory, iProcess.layerCode!!.toInt()))
                currentProcessUnit = iProcess.unit
            }
        }

        val results = data.groupBy { Pair(it.first, it.third) }.map { item ->
            Triple(
                item.key.first,
                item.value.sumOf { it.second },
                item.key.second
            )
        }.toMutableList()

        if (preLayeringProcess != null && layeringProcess != null) {
            val currentLayeringProcess = parentProcesses.firstOrNull { it.processConvertCode == layeringProcess.processConvertCode }
            if (currentLayeringProcess == null) return results

            val currentPreLayeringProcess = parentProcesses.filter { it.processSequence!! < currentLayeringProcess.processSequence!! }
                .sortedByDescending { it.processSequence }.firstOrNull()
            if (currentPreLayeringProcess == null) return results

            val currentResults = results.firstOrNull { it.first == currentPreLayeringProcess.processCode && it.third == currentPreLayeringProcess.layerCode?.toIntOrNull() }
            if (currentResults == null) return results

            var currentInventory = BigDecimal(min(currentResults.second.toDouble(), preLayeringProcess.second.toDouble()))
            var currentProcessUnit = currentPreLayeringProcess.unit
            for (iProcess in parentProcesses.filter { it.processSequence!! >= currentLayeringProcess.processSequence!! }.sortedBy { it.processSequence }) {
                val completionRate = completionRateInfo.firstOrNull { x ->
                    x.processCode == iProcess.processCode && x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
                }?.rate ?: break

                currentInventory = if (currentProcessUnit == ProcessUnit.SHEET) {
                    if (iProcess.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate * BigDecimal(productInfo.shBlock!!)) / BigDecimal(100)
                    }
                } else {
                    if (iProcess.unit == currentProcessUnit) {
                        (currentInventory * completionRate) / BigDecimal(100)
                    } else {
                        (currentInventory * completionRate) / (BigDecimal(productInfo.shBlock!!) * BigDecimal(100))
                    }
                }

                val exist = results.find { it.first == iProcess.processCode!! && it.third == iProcess.layerCode!!.toInt() }
                if (exist != null) {
                    results.remove(exist)
                }
                results.add(Triple(iProcess.processCode!!, currentInventory, iProcess.layerCode!!.toInt()))

                currentProcessUnit = iProcess.unit
            }
        }

        return results
    }

    private fun createPlanProcess(
        processIns: ProductProcessModel,
        orderAllocations: List<AllocationRateByDateModel>,
        processes: List<ProductProcessModel>,
        childrenProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        productInfo: Product,
        processCalculateFromInventories: List<Triple<String?, BigDecimal, Int>>,
        holidays: List<OffsetDateTime>
    ): List<PlanProcessCreateModel> {
        val processCreateModels = mutableListOf<PlanProcessCreateModel>()
        var count = 1

        for (order in orderAllocations.sortedBy { x -> x.orderDate }) {
            var dayOfImplement = processIns.dayOfImplementation ?: 0
            var currentPlanDate = order.orderDate
            for (iProcess in processes.sortedWith(compareBy<ProductProcessModel> { it.layerCode?.toIntOrNull() ?: 0 }
                .thenByDescending { it.dayOfImplementation ?: 0 }
                .thenByDescending { it.processSequence })
            ) {
                val processCalculateFromInventory = processCalculateFromInventories.find { x ->
                    x.first == iProcess.processCode && x.third == iProcess.layerCode!!.toInt()
                }
                if (processCalculateFromInventory == null) continue

                val diffDay = dayOfImplement - (iProcess.dayOfImplementation ?: 0)
                val completionRate = completionRateInfo.find { x ->
                    x.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull() && x.processCode == iProcess.processCode
                }?.rate ?: break
                val planProcess = generatePlanProcessModel(iProcess, completionRate)
                currentPlanDate = currentPlanDate.plusDays((-diffDay).toLong())
                while (holidays.any { x -> x.isEqual(currentPlanDate) }) {
                    currentPlanDate = currentPlanDate.plusDays(-1)
                }

                var sheetQuantity: BigDecimal
                var blockQuantity: BigDecimal
                if (iProcess.unit == ProcessUnit.SHEET) {
                    sheetQuantity = if (count == orderAllocations.size) {
                        processCalculateFromInventory.second - BigDecimal(processCreateModels.filter {
                            it.processCode == iProcess.processCode && it.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
                        }.map { it.planDetails }.flatten().sumOf { it.sheetQuantity ?: 0 })
                    } else {
                        (processCalculateFromInventory.second * order.rate) / BigDecimal(100)
                    }
                    blockQuantity = sheetQuantity * BigDecimal(productInfo.shBlock!!)
                } else {
                    blockQuantity = if (count == orderAllocations.size) {
                        processCalculateFromInventory.second - BigDecimal(processCreateModels.filter {
                            it.processCode == iProcess.processCode && it.layerCode?.toIntOrNull() == iProcess.layerCode?.toIntOrNull()
                        }.map { it.planDetails }.flatten().sumOf { it.blockQuantity ?: 0 })
                    } else {
                        (processCalculateFromInventory.second * order.rate) / BigDecimal(100)
                    }
                    sheetQuantity = blockQuantity / BigDecimal(productInfo.shBlock!!)
                }

                //Chưa tính năng suất máy
                val planDetail = PlanDetailCreateModel(
                    title = PlanTitle.PLAN_KEY,
                    planDate = currentPlanDate,
                    sheetQuantity = NumberHelper.roundedUp(sheetQuantity),
                    blockQuantity = NumberHelper.roundedUp(blockQuantity),
                    orderDate = order.orderDate,
                    hasInventory = true
                )
                planProcess.planDetails = mutableListOf(planDetail)
                planProcess.childrenProcesses = childrenProcesses.filter { x -> x.processInventoryCode == iProcess.processCode }
                    .map { x -> generatePlanChildrenProcessModel(x) }.toMutableList()
                processCreateModels.add(planProcess)
                dayOfImplement = iProcess.dayOfImplementation ?: 0
            }
            count++
        }
        return processCreateModels
    }

    private fun calculatePlanProcessAfterAllocate(
        processIns: ProductProcessModel,
        orderAllocations: List<OrderInfo>,
        parentProcesses: List<ProductProcessModel>,
        childrenProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        productInfo: Product,
        holidays: List<OffsetDateTime>
    ): List<PlanProcessCreateModel> {
        val processCreateModels = mutableListOf<PlanProcessCreateModel>()

        for (iOrder in orderAllocations.sortedBy { x -> x.orderDate }) {
            var processSource = processIns
            var completionRateSource = completionRateInfo.firstOrNull { x -> x.processCode == ProcessCode.INS }
            if (completionRateSource == null) break

            var planDetailSource = mutableListOf(generatePlanDetailINSModel(iOrder, processSource.unit!!))

            var currentLayerCode = processSource.layerCode?.toIntOrNull() ?: 0
            var count = 1
            while (count <= (productInfo.layerCount ?: 0)) {
                val planCalculator = calculatePlanProcess(
                    iOrder.orderDate!!, processSource, completionRateSource!!, planDetailSource, completionRateInfo,
                    parentProcesses.filter { it.layerCode?.toIntOrNull() == currentLayerCode },
                    childrenProcesses, productInfo, listOf(), mutableListOf(), holidays
                )
                processCreateModels.addAll(planCalculator.planProcessResults)

                if (planCalculator.processSource == null) break
                processSource = planCalculator.processSource!!

                completionRateSource = planCalculator.completionRateSource
                planDetailSource = planCalculator.planDetailSource

                val processOfNextLayer = childrenProcesses.find { x ->
                    x.layerCode?.toIntOrNull() != currentLayerCode && x.processConvertCode == processSource.processConvertCode
                }
                if (processOfNextLayer == null) break

                currentLayerCode = processOfNextLayer.layerCode?.toIntOrNull() ?: 0
                count++
            }

            //region check trường hợp ghép lớp gia áp nhiệt
            val processGAN = processCreateModels.firstOrNull { x -> x.processStatisticCode == ProcessStatisticCode.GHEPLOP_GIAAPNHIET }
            if (processGAN != null) {
                processSource = parentProcesses.first { x -> x.layerCode?.toIntOrNull() == processGAN.layerCode?.toIntOrNull() && x.processCode == processGAN.processCode }
                completionRateSource = completionRateInfo.first { x ->
                    x.processCode == processSource.processCode && x.layerCode?.toIntOrNull() == processSource.layerCode?.toIntOrNull()
                }
                planDetailSource = processGAN.planDetails
                val processOfNextLayer = childrenProcesses.find { x ->
                    x.layerCode?.toIntOrNull() != processGAN.layerCode?.toIntOrNull() && x.processConvertCode == processSource.processConvertCode
                }
                if (processOfNextLayer != null) {
                    currentLayerCode = processOfNextLayer.layerCode?.toIntOrNull() ?: 0
                    val planCalculator = calculatePlanProcess(
                        iOrder.orderDate!!, processSource, completionRateSource, planDetailSource, completionRateInfo,
                        parentProcesses.filter { it.layerCode?.toIntOrNull() == currentLayerCode },
                        childrenProcesses, productInfo, listOf(), mutableListOf(), holidays
                    )
                    processCreateModels.addAll(planCalculator.planProcessResults)
                }
            }

        }
        return processCreateModels
    }

    //endregion

    //region SORT_PRODUCT
    private fun sortProduct(
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        inventories: List<InventoryProductResponse>,
        productProcesses: List<ProductProcessModel>
    ): List<Pair<OffsetDateTime, List<OrderInfo>>> {
        val productGroupByDates = mutableListOf<Pair<OffsetDateTime, List<OrderInfo>>>()

        val orderByDateGroup = orderInfo.groupBy { x -> x.orderDate }.toSortedMap(compareBy { it })
        for (orderGrp in orderByDateGroup) {
            val data = mutableListOf<String>()
            if (orderGrp.value.size == 1) {
                data.add(orderGrp.value.first().productName!!)
                productGroupByDates.add(
                    Pair(
                        orderGrp.key!!,
                        data.mapNotNull { x ->
                            val order = orderGrp.value.find { it.productName == x }
                            order
                        }
                    )
                )
                continue
            }

            val productNames = orderGrp.value.mapNotNull { x -> x.productName }.toMutableList()
            val inventoryByProduct = inventories.filter { x ->
                x.inventoryDate!!.isEqual(orderGrp.key) && productNames.contains(x.productName) && x.processCode == ProcessCode.INS
            }
            if (inventoryByProduct.size == 1) {
                data.add(inventoryByProduct.first().productName!!)
                productNames.removeAll { x -> x == inventoryByProduct.first().productName!! }
            }

            if (productNames.size == 1) {
                data.add(productNames.first())
                productGroupByDates.add(
                    Pair(
                        orderGrp.key!!,
                        data.mapNotNull { x ->
                            val order = orderGrp.value.find { it.productName == x }
                            order
                        }
                    )
                )
                continue
            }

            val productSR = productInfo.filter { x -> x.srNosr == SR_OR_NSR.SR && productNames.contains(x.name) }
            if (productSR.isNotEmpty()) {
                data.addAll(sortProductBySR(
                    orderGrp.value.filter { x -> productSR.any { m -> m.name == x.productName } },
                    productSR,
                    productProcesses.filter { x -> productSR.any { m -> m.name == x.productName } }
                ))
            }

            val productCSP = productInfo.filter { x -> x.srNosr == SR_OR_NSR.CSP && productNames.contains(x.name) }
            if (productCSP.isNotEmpty()) {
                data.addAll(sortProductBySR(
                    orderGrp.value.filter { x -> productCSP.any { m -> m.name == x.productName } },
                    productCSP,
                    productProcesses.filter { x -> productCSP.any { m -> m.name == x.productName } }
                ))
            }

            val productNSR = productInfo.filter { x -> x.srNosr == SR_OR_NSR.NSR && productNames.contains(x.name) }
            if (productNSR.isNotEmpty()) {
                data.addAll(sortProductBySR(
                    orderGrp.value.filter { x -> productNSR.any { m -> m.name == x.productName } },
                    productNSR,
                    productProcesses.filter { x -> productNSR.any { m -> m.name == x.productName } }
                ))
            }

            productGroupByDates.add(
                Pair(
                    orderGrp.key!!,
                    data.mapNotNull { x ->
                        val order = orderGrp.value.find { it.productName == x }
                        order
                    }
                )
            )
        }

        return productGroupByDates.sortedBy { x -> x.first }
    }

    private fun sortProductBySR(
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        productProcesses: List<ProductProcessModel>
    ): List<String> {
        if (productInfo.size == 1) {
            return listOf(productInfo.first().name!!)
        }

        val data = mutableListOf<String>()
        val productByLayerCount = productInfo.groupBy { x -> x.layerCount }.toSortedMap(compareByDescending { it })

        for (productGrp in productByLayerCount) {
            if (productGrp.value.size == 1) {
                data.add(productGrp.value.first().name!!)
                continue
            }
            val processGrp = productProcesses.filter { x -> productGrp.value.any { m -> m.name == x.productName } }
                .groupBy { x -> x.productName }
                .map { x -> Pair(x.key, x.value.maxOf { m -> m.dayOfImplementation ?: 0 }) }
                .groupBy { x -> x.second }
                .toSortedMap(compareByDescending { it })

            for (iProcessGrp in processGrp) {
                if (iProcessGrp.value.size == 1) {
                    data.add(iProcessGrp.value.first().first!!)
                    continue
                }
                val orders = orderInfo.filter { x -> iProcessGrp.value.any { m -> m.first == x.productName } }.sortedByDescending { x -> x.quantity }
                data.addAll(orders.mapNotNull { x -> x.productName })
            }
        }

        return data
    }

    //endregion

    //region GENERATE_MODEL
    private fun genPlanValidModel(productName: String, errors: List<String>, date: OffsetDateTime? = null): PlanValidateModel {
        return PlanValidateModel(
            productName = productName,
            date = if (date != null) DateTimeHelper.toString(date, DateTimeFormat.dd_MM_yyyy) else null,
            message = errors.joinToString(separator = "; ")
        )
    }

    private fun generatePlanModel(
        request: CreatePlanRequest,
        planCalendarConfig: PlanCalendarConfig,
        planStartDate: OffsetDateTime,
        planEndDate: OffsetDateTime,
        hasInventory: Boolean
    ): PlanTemp {
        return PlanTemp(
            planCode = "F${DateTimeHelper.toString(planStartDate, DateTimeFormat.yyyyMMdd)}T${DateTimeHelper.toString(planEndDate, DateTimeFormat.yyyyMMdd)}",
            description = request.description ?: "Kế hoạch sản xuất tháng ${planCalendarConfig.month}/${planCalendarConfig.year}",
            month = planCalendarConfig.month,
            year = planCalendarConfig.year,
            startDate = planStartDate,
            endDate = planEndDate,
            version = 0,
            isActive = true,
            hasInventory = hasInventory
        )
    }

    private fun generatePlanProductModel(product: Product): PlanProductCreateModel {
        return PlanProductCreateModel(
            productName = product.name,
            frame_1 = product.frame_1,
            mold = product.mold,
            pcsSh = product.pcsSh,
            blockSh = product.shBlock,
            planProcesses = mutableListOf()
        )
    }

    private fun generatePlanProductModel(
        planProduct: PlanProductTemp,
        planProcesses: List<PlanProcessTemp>,
        planDetails: List<PlanDetailTemp>
    ): PlanProductCreateModel {
        val parentPlanProcesses = planProcesses.filter { it.parentId.isNullOrEmpty() }
        val childrenPlanProcesses = planProcesses.filter { !it.parentId.isNullOrEmpty() }
        return PlanProductCreateModel(
            productName = planProduct.productName,
            frame_1 = planProduct.frame_1,
            mold = planProduct.mold,
            pcsSh = planProduct.pcsSh,
            blockSh = planProduct.blockSh,
            planProcesses = parentPlanProcesses.map { item ->
                val result = PlanProcessCreateModel(
                    processCode = item.processCode,
                    processName = item.processName,
                    processConvertCode = item.processConvertCode,
                    layerCode = item.layerCode,
                    completionRate = item.completionRate,
                    inventory = item.inventory,
                    unit = item.unit,
                    processSequence = item.processSequence,
                    processNameJp = item.processNameJp,
                    processGroup = item.processGroup,
                    processStatisticCode = item.processStatisticCode,
                    childrenProcesses = childrenPlanProcesses.filter { x -> x.parentId == item.id }.map { x ->
                        PlanChildrenProcessCreateModel(
                            processCode = x.processCode,
                            processName = x.processName,
                            processConvertCode = x.processConvertCode,
                            layerCode = x.layerCode,
                            completionRate = x.completionRate,
                            inventory = x.inventory,
                            unit = x.unit,
                            processSequence = x.processSequence,
                            processNameJp = x.processNameJp,
                            processGroup = x.processGroup,
                            processStatisticCode = x.processStatisticCode
                        )
                    }.toMutableList()
                )
//                val iWrs = workResults.filter { it.processCode == item.processCode && it.layerCode?.toIntOrNull() == item.layerCode?.toIntOrNull() }
//                if (replan) {
//                    result.planDetails = iWrs.groupBy { Triple(it.processCode, it.layerCode, DateTimeHelper.toString(it.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map {x ->
//                        val firstValue = x.value.first()
//                        PlanDetailCreateModel(
//                            title = PlanTitle.PLAN_KEY,
//                            planDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
//                            sheetQuantity = x.value.sumOf { it.goodSheetQuantity ?: 0 },
//                            blockQuantity = x.value.sumOf { it.goodTapeQuantity ?: 0 },
//                            orderDate = DateTimeHelper.toUniversalTime(firstValue.summaryResultDate!!),
//                            hasInventory = true
//                        )
//                    }.toMutableList()
//                } else {
//
//                }
                result.planDetails = planDetails.filter { x -> x.planProcessId == item.id }.map { x ->
                    PlanDetailCreateModel(
                        title = x.title,
                        planDate = x.planDate,
                        sheetQuantity = x.sheetQuantity,
                        blockQuantity = x.blockQuantity,
                        orderDate = x.orderDate,
                        hasInventory = x.hasInventory
                    )
                }.toMutableList()
                result
            }.toMutableList()
        )
    }

    private fun mappingPlanProductModel(planProducts: List<PlanProductCreateModel>): MutableList<PlanProductCreateModel> {
        return planProducts.groupBy { x -> x.productName }.map { x ->
            val product = x.value.first()
            PlanProductCreateModel(
                productName = x.key,
                frame_1 = product.frame_1,
                mold = product.mold,
                pcsSh = product.pcsSh,
                blockSh = product.blockSh,
                planProcesses = x.value.asSequence().map { m -> m.planProcesses }.flatten().groupBy { m -> Pair(m.processCode, m.layerCode) }.map { m ->
                    val process = m.value.first()
                    val result = PlanProcessCreateModel(
                        processCode = m.key.first,
                        processName = process.processName,
                        processConvertCode = process.processConvertCode,
                        layerCode = m.key.second,
                        completionRate = process.completionRate,
                        inventory = process.inventory,
                        unit = process.unit,
                        processSequence = process.processSequence,
                        processNameJp = process.processNameJp,
                        processGroup = process.processGroup,
                        processStatisticCode = process.processStatisticCode,
                        childrenProcesses = process.childrenProcesses,
                        planDetails = m.value.map { t -> t.planDetails }.flatten().toMutableList(),
                    )
                    //result.planDetails.addAll(calculateAccumulation(result.planDetails))
                    result
                }.toMutableList()
            )
        }.toMutableList()
    }

    private fun generatePlanProcessModel(process: ProductProcessModel, completionRate: BigDecimal?, inventory: Int? = 0): PlanProcessCreateModel {
        return PlanProcessCreateModel(
            processCode = process.processCode,
            processName = process.processName,
            processConvertCode = process.processConvertCode,
            layerCode = process.layerCode,
            completionRate = completionRate,
            inventory = inventory ?: 0,
            unit = process.unit,
            processSequence = process.processSequence,
            processNameJp = process.processNameJp,
            processGroup = process.processGroup,
            processStatisticCode = process.processStatisticCode
        )
    }

    private fun generatePlanChildrenProcessModel(productProcess: ProductProcessModel): PlanChildrenProcessCreateModel {
        return PlanChildrenProcessCreateModel(
            processCode = productProcess.processCode,
            processName = productProcess.processName,
            processConvertCode = productProcess.processConvertCode,
            layerCode = productProcess.layerCode,
            inventory = 0,
            unit = productProcess.unit,
            processSequence = productProcess.processSequence,
            processNameJp = productProcess.processNameJp,
            processGroup = productProcess.processGroup,
            processStatisticCode = productProcess.processStatisticCode,
        )
    }

    private fun mappingPlanProcessModel(planProcesses: MutableList<PlanProcessCreateModel>): MutableList<PlanProcessCreateModel> {
        val data = mutableListOf<PlanProcessCreateModel>()
        for (item in planProcesses.groupBy { x -> Pair(x.processCode, x.layerCode) }) {
            val process = item.value.first()
            val result = PlanProcessCreateModel(
                processCode = item.key.first,
                processName = process.processName,
                processConvertCode = process.processConvertCode,
                layerCode = item.key.second,
                completionRate = process.completionRate,
                inventory = process.inventory,
                unit = process.unit,
                processSequence = process.processSequence,
                processNameJp = process.processNameJp,
                processGroup = process.processGroup,
                processStatisticCode = process.processStatisticCode,
                childrenProcesses = process.childrenProcesses.distinct().toMutableList(),
                planDetails = item.value.asSequence().map { m -> m.planDetails }.flatten().groupBy { m -> m.planDate }.map { m ->
                    PlanDetailCreateModel(
                        title = m.value.first().title,
                        planDate = m.key,
                        sheetQuantity = m.value.sumOf { t -> t.sheetQuantity ?: 0 },
                        blockQuantity = m.value.sumOf { t -> t.blockQuantity ?: 0 }
                    )
                }.toMutableList()
            )
            data.add(result)
        }


        return data
    }

    private fun generatePlanDetailINSModel(orderInfo: OrderInfo, unit: String): PlanDetailCreateModel {
        return if (unit == ProcessUnit.SHEET) {
            PlanDetailCreateModel(
                title = PlanTitle.PLAN_KEY,
                planDate = orderInfo.orderDate,
                sheetQuantity = orderInfo.quantity ?: 0,
                blockQuantity = (orderInfo.quantity ?: 0) * (orderInfo.blockSh ?: 0)
            )
        } else {
            PlanDetailCreateModel(
                title = PlanTitle.PLAN_KEY,
                planDate = orderInfo.orderDate,
                sheetQuantity = (orderInfo.quantity ?: 0) / (orderInfo.blockSh ?: 0),
                blockQuantity = orderInfo.quantity ?: 0
            )
        }
    }

    private fun generatePlanDetailModel(
        orderDate: OffsetDateTime,
        currentPlanDetail: List<PlanDetailCreateModel>,
        currentProcessUnit: String,
        productProcess: ProductProcessModel,
        diffDay: Int,
        productInfo: Product,
        completionRate: BigDecimal,
        equipmentInfoDefault: EquipmentProductivity,
        equipmentUsedInfo: List<Pair<OffsetDateTime, EquipmentProductivity>>,
        holidays: List<OffsetDateTime>
    ): MutableList<PlanDetailCreateModel> {
        val data = mutableListOf<PlanDetailCreateModel>()

        for (iCurrPlanDetail in currentPlanDetail) {
            var planDate = iCurrPlanDetail.planDate!!.plusDays((-diffDay).toLong())
            var sheetQuantity: BigDecimal
            var blockQuantity: BigDecimal

            if (currentProcessUnit == ProcessUnit.SHEET) {
                sheetQuantity = BigDecimal(iCurrPlanDetail.sheetQuantity!! * 100) / completionRate
                blockQuantity = sheetQuantity * BigDecimal(productInfo.shBlock!!)
            } else {
                blockQuantity = BigDecimal(iCurrPlanDetail.blockQuantity!! * 100) / completionRate
                sheetQuantity = blockQuantity / BigDecimal(productInfo.shBlock!!)
            }

            if (applyEquipmentProductivity) {
                while (sheetQuantity > BigDecimal(0) || blockQuantity > BigDecimal(0)) {
                    if (holidays.any { x -> x.isEqual(planDate) }) {
                        planDate = planDate.plusDays(-1)
                        continue
                    }
                    val eqUsedConfig = equipmentUsedInfo.firstOrNull { x ->
                        x.first == planDate && x.second.grpProcess == productProcess.processGroup
                            && x.second.frame_1 == productInfo.frame_1 && x.second.mold?.contains(productInfo.mold!!) == true
                    }?.second
                    val eqConfig = settingEquipmentConfig(eqUsedConfig, equipmentInfoDefault)
                    val planDetail = calculateQuantity(planDate, sheetQuantity, blockQuantity, eqConfig, productProcess.unit!!, productInfo.shBlock!!)
                    data.add(planDetail)
                    planDate = planDate.plusDays(-1)
                    sheetQuantity -= NumberHelper.toDecimal(planDetail.sheetQuantity!!)
                    blockQuantity -= NumberHelper.toDecimal(planDetail.blockQuantity!!)
                }
            }
            else {
                while (holidays.any { x -> x.isEqual(planDate) }) {
                    planDate = planDate.plusDays(-1)
                }
                val planDetail = PlanDetailCreateModel(
                    title = PlanTitle.PLAN_KEY,
                    planDate = planDate,
                    sheetQuantity = NumberHelper.roundedUp(sheetQuantity),
                    blockQuantity = NumberHelper.roundedUp(blockQuantity),
                    orderDate = orderDate,
                    hasInventory = false
                )
                data.add(planDetail)
            }
        }
        return data
    }

    private fun settingEquipmentConfig(equipmentUsedInfo: EquipmentProductivity?, equipmentInfoDefault: EquipmentProductivity): EquipmentProductivity {
        val data = EquipmentProductivity(
            id = equipmentInfoDefault.id,
            processName = equipmentInfoDefault.processName,
            description = equipmentInfoDefault.description,
            frame_1 = equipmentInfoDefault.frame_1,
            grpProcess = equipmentInfoDefault.grpProcess,
            mold = equipmentInfoDefault.mold,
            operatingRate = equipmentInfoDefault.operatingRate,
            time = equipmentInfoDefault.time,
            count = equipmentInfoDefault.count,
            task = equipmentInfoDefault.task,
            sheetHour_100 = equipmentInfoDefault.sheetHour_100,
            blockSh = equipmentInfoDefault.blockSh,
            sltbHour = equipmentInfoDefault.sltbHour,
            sltbSheet = (equipmentInfoDefault.sltbSheet ?: BigDecimal(0)) - (equipmentUsedInfo?.sltbSheet ?: BigDecimal(0)),
            sltbSet = equipmentInfoDefault.sltbSet,
            sltbBlock = (equipmentInfoDefault.sltbBlock ?: BigDecimal(0)) - (equipmentUsedInfo?.sltbBlock ?: BigDecimal(0)),
            capHour = equipmentInfoDefault.capHour,
            capSheet = equipmentInfoDefault.capSheet,
            capSet = equipmentInfoDefault.capSet,
            capBlock = equipmentInfoDefault.capBlock,
            quantityMachine = equipmentInfoDefault.quantityMachine,
            processCode = equipmentInfoDefault.processCode
        )
        return data
    }

    private fun settingEquipmentConfig(planDetail: PlanDetailCreateModel, equipmentInfoDefault: EquipmentProductivity): EquipmentProductivity {
        val data = EquipmentProductivity(
            id = equipmentInfoDefault.id,
            processName = equipmentInfoDefault.processName,
            description = equipmentInfoDefault.description,
            frame_1 = equipmentInfoDefault.frame_1,
            grpProcess = equipmentInfoDefault.grpProcess,
            mold = equipmentInfoDefault.mold,
            operatingRate = equipmentInfoDefault.operatingRate,
            time = equipmentInfoDefault.time,
            count = equipmentInfoDefault.count,
            task = equipmentInfoDefault.task,
            sheetHour_100 = equipmentInfoDefault.sheetHour_100,
            blockSh = equipmentInfoDefault.blockSh,
            sltbHour = equipmentInfoDefault.sltbHour,
            sltbSheet = (equipmentInfoDefault.sltbSheet ?: BigDecimal(0)) - BigDecimal((planDetail.sheetQuantity ?: 0)),
            sltbSet = equipmentInfoDefault.sltbSet,
            sltbBlock = (equipmentInfoDefault.sltbBlock ?: BigDecimal(0)) - BigDecimal((planDetail.blockQuantity ?: 0)),
            capHour = equipmentInfoDefault.capHour,
            capSheet = equipmentInfoDefault.capSheet,
            capSet = equipmentInfoDefault.capSet,
            capBlock = equipmentInfoDefault.capBlock,
            quantityMachine = equipmentInfoDefault.quantityMachine,
            processCode = equipmentInfoDefault.processCode
        )
        return data
    }

    private fun calculateQuantity(
        planDate: OffsetDateTime,
        sheetQuantity: BigDecimal,
        blockQuantity: BigDecimal,
        equipmentInfo: EquipmentProductivity,
        unit: String,
        blockSh: Int
    ): PlanDetailCreateModel {
        val planDetail = PlanDetailCreateModel(title = PlanTitle.PLAN_KEY, planDate = planDate)
        when (unit) {
            ProcessUnit.SHEET -> {
                if ((equipmentInfo.sltbSheet ?: BigDecimal(0)) <= sheetQuantity) {
                    planDetail.sheetQuantity = NumberHelper.roundedUp(equipmentInfo.sltbSheet ?: BigDecimal(0))
                    planDetail.blockQuantity = (planDetail.sheetQuantity ?: 0) * blockSh
                } else {
                    planDetail.sheetQuantity = NumberHelper.roundedUp(sheetQuantity)
                    planDetail.blockQuantity = NumberHelper.roundedUp(blockQuantity)
                }
            }

            ProcessUnit.BLOCK -> {
                if ((equipmentInfo.sltbBlock ?: BigDecimal(0)) <= blockQuantity) {
                    planDetail.blockQuantity = NumberHelper.roundedUp(equipmentInfo.sltbBlock ?: BigDecimal(0))
                    planDetail.sheetQuantity = NumberHelper.roundedUp(NumberHelper.toDecimal(planDetail.blockQuantity ?: 0) / NumberHelper.toDecimal(blockSh))
                } else {
                    planDetail.sheetQuantity = NumberHelper.roundedUp(sheetQuantity)
                    planDetail.blockQuantity = NumberHelper.roundedUp(blockQuantity)
                }
            }
        }
        return planDetail
    }

    private fun calculateEquipmentUsedInfo(
        planProcesses: List<PlanProcessCreateModel>,
        productInfo: Product,
        equipmentInfoDefault: List<EquipmentProductivity>,
        equipmentUsedInfo: List<Pair<OffsetDateTime, EquipmentProductivity>>
    ): MutableList<Pair<OffsetDateTime, EquipmentProductivity>> {
        val data = equipmentUsedInfo.map { it }.toMutableList()

        for (process in planProcesses) {
            for (detail in process.planDetails) {
                val eqUsedConfig = data.firstOrNull { x ->
                    x.first == detail.planDate && x.second.grpProcess == process.processGroup
                        && x.second.frame_1 == productInfo.frame_1 && x.second.mold?.contains(productInfo.mold!!) == true
                }
                if (eqUsedConfig != null) {
                    val newEqConfig = settingEquipmentConfig(detail, eqUsedConfig.second)
                    data.remove(eqUsedConfig)
                    data.add(Pair(detail.planDate!!, newEqConfig))
                } else {
                    val eqConfigDefault = equipmentInfoDefault.firstOrNull { x ->
                        x.grpProcess == process.processGroup
                            && x.frame_1 == productInfo.frame_1 && x.mold?.contains(productInfo.mold!!) == true
                    }
                    if (eqConfigDefault == null) continue
                    val newEqConfig = settingEquipmentConfig(detail, eqConfigDefault)
                    data.add(Pair(detail.planDate!!, newEqConfig))
                }
            }
        }

        return data
    }

    //endregion
}