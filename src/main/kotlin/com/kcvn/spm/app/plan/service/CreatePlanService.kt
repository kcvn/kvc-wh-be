package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.plan.payload.model.PlanChildrenProcessCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanDetailCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanProcessCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanProductCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanValidateModel
import com.kcvn.spm.app.plan.payload.request.CreatePlanRequest
import com.kcvn.spm.app.productprocess.payload.model.ProductProcessModel
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.Frame1
import com.kcvn.spm.common.constants.PlanTitle
import com.kcvn.spm.common.constants.ProcessCode
import com.kcvn.spm.common.constants.ProcessConvertCode
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.constants.ProcessUnit
import com.kcvn.spm.common.constants.SR_OR_NSR
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
import com.kcvn.spm.model.tables.pojos.PlanTemp
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.repository.EquipmentProductivityRepository
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.PlanRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.SystemLockRepository
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
    private val planRep: PlanRepository
) {

    private val typeOfSystemLocks = listOf(
        Constants.SYSTEM_LOCK_CREATE_PLAN,
        Constants.SYSTEM_LOCK_IMPORT_ORDER,
        Constants.SYSTEM_LOCK_PRODUCT_PROCESS
    )

    fun checkInventory(request: CreatePlanRequest): BaseResponse<Boolean> {
        if (request.startDate == null) throw BusinessException("Chưa có thông tin ngày bắt đầu và ngày kết thúc kế hoạch")
        val inventory = inventoryProductRep.findDateInventoryProduct(request.startDate!!)
        return BaseResponse(inventory != null)
    }

    //region VALIDATE
    fun createPlan(request: CreatePlanRequest): BaseResponse<FileContentModel> {
        if (systemLockRep.isLock(Constants.SYSTEM_LOCK_CREATE_PLAN))
            throw BusinessException("Chức năng này đang bị khóa tạm thời. Vui lòng thử lại sau ít phút nữa")

        if (request.startDate == null || request.endDate == null)
            throw BusinessException("Chưa có thông tin ngày bắt đầu và ngày kết thúc kế hoạch")

        if (request.inventoryDate != null && (request.inventoryDate!! < request.startDate || request.inventoryDate!! > request.endDate))
            throw BusinessException("Ngày chốt tồn kho đang nằm ngoài khoảng thời gian của tháng sản xuất")

        val startDate = request.startDate!!
        val endDate = request.endDate!!

        val orderInfo = orderInfoRep.getOrderInfoByTimeRange(startDate, endDate)
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
                    if (productProcess.any { x -> x.dayOfImplementation == null || x.dayOfImplementation == 0 }) errors.add("Dữ liệu công đoạn chưa đầy đủ Ngày thứ thực hiện")
                }
                val parentProcesses = productProcess.filter { x -> !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO }
                val processNotCompletionRate = parentProcesses.filter { x ->
                    !completionRateInfo.any { m ->
                        m.productNameShortcut == item.substring(item.length - 7, item.length)
                            && m.processCode == x.processCode
                    }
                }
                if (processNotCompletionRate.isNotEmpty()) {
                    val strProcess = processNotCompletionRate.map { x -> x.processCode }.joinToString(separator = ", ")
                    errors.add("Chưa có thông tin tỷ lệ đạt của các công đoạn $strProcess")
                }

                val equipmentByProducts = equipmentInfo.filter { x -> x.frame_1 == product!!.frame_1 }
                if (equipmentByProducts.isEmpty()) {
                    errors.add("Chưa có cấu hình năng suất máy cho line ${product!!.frame_1}")
                } else {
                    for (process in parentProcesses) {
                        var eqConfigs = equipmentByProducts.filter { x -> x.grpProcess == process.processGroup }
                        if (eqConfigs.isEmpty()) {
                            errors.add("Chưa có cấu hình năng suất máy cho nhóm công đoạn ${process.processGroup}")
                        } else {
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
                if (request.inventoryDate == null) {
                    createPlanNoInventory(request, orderInfo, productInfo, productProcesses, completionRateInfo, equipmentInfo)
                } else {
                    createPlanNoInventory(request, orderInfo, productInfo, productProcesses, completionRateInfo, equipmentInfo)
                }
                return BaseResponse(message = "Tạo kế hoạch thành công")
            }

            val fileContent = exportFilePlanValidate(planValidates)
            systemLockRep.unlock(typeOfSystemLocks)
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

    //region CREATE_PLAN
    private fun createPlanNoInventory(
        request: CreatePlanRequest,
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        productProcesses: List<ProductProcessModel>,
        completionRateInfo: List<CompletionRateProcessProduct>,
        equipmentInfo: List<EquipmentProductivity>
    ) {
        try {
            val productGroupByDates = sortProduct(orderInfo, productInfo, listOf(), productProcesses)
            val planProducts = mutableListOf<PlanProductCreateModel>()
            val endDate = orderInfo.sortedByDescending { x -> x.orderDate }.first().orderDate!!
            var startDate = endDate
            for (iProdByDate in productGroupByDates) {
                val orders = iProdByDate.second
                val equipmentConfigByDate = mutableListOf<EquipmentProductivity>()
                for (iOrder in orders) {
                    val product = productInfo.firstOrNull { x -> x.name == iOrder.productName }
                    val processes = productProcesses.filter { x -> x.productName == iOrder.productName }
                    val processIns = processes.firstOrNull { x -> x.processConvertCode == ProcessConvertCode.INS }

                    if (processes.isEmpty() || product == null || processIns == null) continue

                    val completionRates = completionRateInfo.filter { x ->
                        x.productNameShortcut == iOrder.productName!!.substring(iOrder.productName!!.length - 7, iOrder.productName!!.length)
                    }

                    val planProductCreateModel = generatePlanProductModel(product)

                    val parentProcesses = processes.filter { x ->
                        !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO
                            && x.processConvertCode != processIns.processConvertCode
                    }.sortedByDescending { x -> x.dayOfImplementation ?: 0 }
                    val childrenProcesses = processes.filter { x ->
                        !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode == ProcessStatisticCode.KO
                            && x.processConvertCode != processIns.processConvertCode
                    }.sortedByDescending { x -> x.dayOfImplementation ?: 0 }

                    val completionRateIns = completionRates.find { x ->
                        x.layerCode == processIns.layerCode && x.processCode == processIns.processCode
                    }
                    if (completionRateIns?.rate == null || completionRateIns.rate!! <= BigDecimal(0)) continue

                    val planProcessINS = generatePlanProcessModel(processIns, completionRateIns.rate, 0)
                    var currentPlanDetail = mutableListOf(generatePlanDetailINSModel(iOrder, processIns.unit!!))
                    planProcessINS.planDetails = currentPlanDetail
                    planProductCreateModel.planProcesses.add(planProcessINS)

                    var currentProcessUnit = processIns.unit!!
                    var dayOfImplement = processIns.dayOfImplementation ?: 0
                    for (iParentProcess in parentProcesses) {
                        val diffDay = dayOfImplement - (iParentProcess.dayOfImplementation ?: 0)
                        val completionRate = completionRates.find { x ->
                            x.layerCode == iParentProcess.layerCode && x.processCode == iParentProcess.processCode
                        } ?: break
                        val eqConfig = equipmentInfo.find { x ->
                            x.frame_1 == product.frame_1 && x.grpProcess == iParentProcess.processGroup && x.mold!!.contains(product.mold!!)
                        } ?: break

                        val planProcess = generatePlanProcessModel(iParentProcess, completionRate.rate, 0)
                        currentPlanDetail = generatePlanDetailModel(
                            currentPlanDetail,
                            currentProcessUnit,
                            iParentProcess.unit!!,
                            diffDay,
                            product.shBlock!!,
                            completionRate?.rate ?: BigDecimal(0),
                            eqConfig
                        )
                        planProcess.planDetails = currentPlanDetail
                        planProcess.childrenProcesses = childrenProcesses.filter { x -> x.processInventoryCode == iParentProcess.processCode }
                            .map { x -> generatePlanChildrenProcessModel(x) }.toMutableList()
                        planProductCreateModel.planProcesses.add(planProcess)

                        dayOfImplement = (iParentProcess.dayOfImplementation ?: 0)
                        currentProcessUnit = iParentProcess.unit!!

                        val minDate =  planProcess.planDetails.minOf { x -> x.planDate!! }
                        if (minDate.isBefore(startDate)) {
                            startDate = minDate
                        }
                    }

                    planProductCreateModel.planProcesses = mappingPlanProcessModel(planProductCreateModel.planProcesses)
                    planProducts.add(planProductCreateModel)
                }
            }

            planRep.createPlanTemp(generatePlanModel(request, startDate, endDate), planProducts.toList())

        } catch (e: Exception) {
            throw e
        } finally {
            systemLockRep.unlock(typeOfSystemLocks)
        }
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

    private fun generatePlanModel(request: CreatePlanRequest, planStartDate: OffsetDateTime, planEndDate: OffsetDateTime): PlanTemp {
        return PlanTemp(
            planCode = "F${DateTimeHelper.toString(planStartDate, DateTimeFormat.yyyyMMdd)}T${DateTimeHelper.toString(planEndDate, DateTimeFormat.yyyyMMdd)}",
            description = request.description ?: "Kế hoạch sản xuất tháng ${request.month}/${request.year}",
            month = request.month,
            year = request.year,
            startDate = planStartDate,
            endDate = planEndDate,
            version = 0,
            isActive = true
        )
    }

    private fun generatePlanProductModel(product: Product): PlanProductCreateModel {
        return PlanProductCreateModel(
            productName = product.name,
            frame_1 = product.frame_1,
            mold = product.mold,
            pcsSh = product.pcsSh,
            blockSh = product.shBlock
        )
    }

    private fun generatePlanProcessModel(process: ProductProcessModel, completionRate: BigDecimal?, inventory: Int?): PlanProcessCreateModel {
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

    private fun generatePlanChildrenProcessModel(productProcess: ProductProcessModel): PlanChildrenProcessCreateModel{
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
        val data = planProcesses.groupBy { x -> x.processCode }
            .map { x ->
                val process = x.value.first()
                val result = PlanProcessCreateModel(
                    processCode = x.key,
                    processName = process.processName,
                    processConvertCode = process.processConvertCode,
                    layerCode = process.layerCode,
                    completionRate = process.completionRate,
                    inventory = process.inventory,
                    unit = process.unit,
                    processSequence = process.processSequence,
                    processNameJp = process.processNameJp,
                    processGroup = process.processGroup,
                    processStatisticCode = process.processStatisticCode,
                    childrenProcesses = process.childrenProcesses,
                    planDetails = x.value.asSequence().map { m -> m.planDetails }.flatten().groupBy { m -> m.planDate }.map { m ->
                        PlanDetailCreateModel (
                            title = m.value.first().title,
                            planDate = m.key,
                            sheetQuantity = m.value.sumOf { t -> t.sheetQuantity ?: 0 },
                            blockQuantity = m.value.sumOf { t -> t.blockQuantity ?: 0 }
                        )
                    }.toMutableList()
                )
                val accumulations = calculateAccumulation(result.planDetails)
                result.planDetails.addAll(accumulations)
                result
            }.toMutableList()

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
        currentPlanDetail: List<PlanDetailCreateModel>,
        currentProcessUnit: String,
        processUnit: String,
        diffDay: Int,
        blockSh: Int,
        completionRate: BigDecimal,
        equipmentInfo: EquipmentProductivity
    ): MutableList<PlanDetailCreateModel> {
        val data = mutableListOf<PlanDetailCreateModel>()

        for (iCurrPlanDetail in currentPlanDetail) {
            var planDate = iCurrPlanDetail.planDate!!.plusDays((-diffDay).toLong())
            var sheetQuantity: Int
            var blockQuantity: Int

            if (currentProcessUnit == ProcessUnit.SHEET) {
                sheetQuantity = NumberHelper.roundedUp((BigDecimal(iCurrPlanDetail.sheetQuantity!! * 100) / completionRate))
                blockQuantity = sheetQuantity * blockSh
            } else {
                blockQuantity = NumberHelper.roundedUp((BigDecimal(iCurrPlanDetail.blockQuantity!! * 100) / completionRate))
                sheetQuantity = blockQuantity / blockSh
            }

            while (sheetQuantity > 0 || blockQuantity > 0) {
                val planDetail = calculateQuantity(planDate, sheetQuantity, blockQuantity, equipmentInfo, processUnit, blockSh)
                data.add(planDetail)
                planDate = planDate.plusDays(-1)
                sheetQuantity -= planDetail.sheetQuantity!!
                blockQuantity -= planDetail.blockQuantity!!
            }

        }

        return data
    }

    private fun calculateQuantity(
        planDate: OffsetDateTime,
        sheetQuantity: Int,
        blockQuantity: Int,
        equipmentInfo: EquipmentProductivity,
        unit: String,
        blockSh: Int
    ): PlanDetailCreateModel {
        val planDetail = PlanDetailCreateModel(title = PlanTitle.PLAN_KEY, planDate = planDate)
        when (unit) {
            ProcessUnit.SHEET -> {
                if ((equipmentInfo.sltbSheet ?: BigDecimal(0)) <= BigDecimal(sheetQuantity)) {
                    planDetail.sheetQuantity = NumberHelper.roundedUp(equipmentInfo.sltbSheet ?: BigDecimal(0))
                    planDetail.blockQuantity = (planDetail.sheetQuantity ?: 0) * blockSh
                } else {
                    planDetail.sheetQuantity = sheetQuantity
                    planDetail.blockQuantity = blockQuantity
                }
            }

            ProcessUnit.BLOCK -> {
                if ((equipmentInfo.sltbBlock ?: BigDecimal(0)) <= BigDecimal(blockQuantity)) {
                    planDetail.blockQuantity = NumberHelper.roundedUp(equipmentInfo.sltbBlock ?: BigDecimal(0))
                    planDetail.sheetQuantity = (planDetail.blockQuantity ?: 0) / blockSh
                } else {
                    planDetail.sheetQuantity = sheetQuantity
                    planDetail.blockQuantity = blockQuantity
                }
            }
        }
        return planDetail
    }

    private fun calculateAccumulation(planDetails: MutableList<PlanDetailCreateModel>): MutableList<PlanDetailCreateModel> {
        var sheetQuantity = 0
        var blockQuantity = 0
        val data = mutableListOf<PlanDetailCreateModel>()
        for (item in planDetails) {
            sheetQuantity += (item.sheetQuantity ?: 0)
            blockQuantity += (item.blockQuantity ?: 0)
            data.add(PlanDetailCreateModel(
                title = PlanTitle.PLAN_ACCUMULATION_KEY,
                planDate = item.planDate,
                sheetQuantity = sheetQuantity,
                blockQuantity = blockQuantity
            ))
        }
        return data
    }

    //endregion
}