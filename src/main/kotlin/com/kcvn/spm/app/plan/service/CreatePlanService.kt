package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
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
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.model.tables.pojos.PlanDetailTemp
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
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
    fun validate(request: CreatePlanRequest): BaseResponse<FileContentModel> {
        if (systemLockRep.isLock(Constants.SYSTEM_LOCK_CREATE_PLAN))
            throw BusinessException("Chức năng này đang bị khóa tạm thời. Vui lòng thử lại sau ít phút nữa")

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

        systemLockRep.lock(typeOfSystemLocks)

        try {
            val lstDate = DateTimeHelper.toCalendarColumn(startDate, endDate)

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
                return BaseResponse()
            }

            val fileContent = exportFilePlanValidate(planValidates)
            systemLockRep.unlock(typeOfSystemLocks)
            return BaseResponse(fileContent)

        } catch (e: Exception) {
            throw e
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

    private fun genPlanValidModel(productName: String, errors: List<String>, date: OffsetDateTime? = null): PlanValidateModel {
        return PlanValidateModel(
            productName = productName,
            date = if (date != null) DateTimeHelper.toString(date, DateTimeFormat.dd_MM_yyyy) else null,
            message = errors.joinToString(separator = "; ")
        )
    }

    //endregion

    //region CREATE_PLAN
    fun createPlan(request: CreatePlanRequest) {
        try {
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

            var productNames = orderInfo.mapNotNull { it.productName }.sortedBy { it }.distinct()
            val productShortcutNames = productNames.map { it.substring(it.length - 7, it.length) }
            val productInfo = productRep.getByName(productNames)
            val productProcesses = productProcessRep.getByProductName(productNames).filter { x -> x.processCode?.toIntOrNull() != 0 }

            val completionRateInfo = completionRateProcessProductRep.getByProductName(productShortcutNames)

            val processGroupCodes = productProcesses.mapNotNull { x -> x.processGroup }
            val equipmentInfo = equipmentProductivityRep.getByProcessGroup(processGroupCodes)

            val inventories = inventoryProductRep.getByProductName(productNames, startDate)

            productNames = sortProduct(orderInfo, productInfo, inventories, productProcesses)

            val planProducts = mutableListOf<PlanProductTemp>()
            val planProcesses = mutableListOf<Pair<String, List<PlanProcessTemp>>>()
            val planChildrenProcesses = mutableListOf<Pair<String, List<PlanProcessTemp>>>()
            val planDetails = mutableListOf<Pair<String, List<PlanDetailTemp>>>()

            for (item in productNames) {
                val processes = productProcesses.filter { x -> x.productName == item }
                val processIns = processes.firstOrNull { x -> x.processConvertCode == ProcessConvertCode.INS }
                val orders = orderInfo.filter { x -> x.productName == item }
                val product = productInfo.firstOrNull { x -> x.name == item }
                val completionRates = completionRateInfo.filter { x -> x.productNameShortcut == item.substring(item.length - 7, item.length) }
                if (processes.isEmpty() || orders.isEmpty() || product == null || processIns == null) continue

                val processInserts = mutableListOf<PlanProcessTemp>()
                val details = mutableListOf<PlanDetailTemp>()

                val parentProcesses = processes.filter { x -> !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode != ProcessStatisticCode.KO }
                    .sortedByDescending { x -> x.dayOfImplementation ?: 0 }
                val childrenProcesses = processes.filter { x -> !x.processStatisticCode.isNullOrEmpty() && x.processStatisticCode == ProcessStatisticCode.KO }
                    .sortedByDescending { x -> x.dayOfImplementation ?: 0 }

                planProducts.add(generatePlanProductModel(product))

                val inventoryIns = inventories.firstOrNull { x -> x.productName == item && x.processCode == processIns.processCode && x.layerCode == processIns.layerCode }
                val completionRateIns = completionRates.find { x ->
                    x.layerCode == processIns.layerCode && x.processCode == processIns.processCode
                        && x.effectiveDate != null && startDate >= x.effectiveDate && (x.expirationDate == null || startDate <= x.expirationDate)
                }
                if (completionRateIns?.rate == null || completionRateIns.rate!! <= BigDecimal(0)) continue

                processInserts.add(
                    generatePlanProcessModel(
                        processIns,
                        completionRateIns.rate,
                        (if (processIns.unit == ProcessUnit.SHEET) inventoryIns?.sheetQuantity else inventoryIns?.productQuantity)
                    )
                )
                var currentPlanDetail = generatePlanDetailINSModel(orders, processIns.unit!!, product.shBlock!!)
                details.addAll(currentPlanDetail)

                var dayOfImplement = processIns.dayOfImplementation
                for (iParentProcesses in parentProcesses.filter { x -> x.layerCode == processIns.layerCode && x.processCode != processIns.processCode }) {
                    val inventory = inventories.firstOrNull { x ->
                        x.productName == item
                            && x.processCode == iParentProcesses.processCode
                            && x.layerCode == iParentProcesses.layerCode
                    }
                    val completionRate = completionRates.find { x ->
                        x.layerCode == iParentProcesses.layerCode && x.processCode == iParentProcesses.processCode
                            && x.effectiveDate != null && startDate >= x.effectiveDate && (x.expirationDate == null || startDate <= x.expirationDate)
                    }
                    if (completionRate?.rate == null || completionRate.rate!! <= BigDecimal(0)) continue

                    processInserts.add(
                        generatePlanProcessModel(
                            iParentProcesses,
                            completionRate.rate,
                            (if (iParentProcesses.unit == ProcessUnit.SHEET) inventory?.sheetQuantity else inventory?.productQuantity)
                        )
                    )

                }

                planProcesses.add(Pair(item, processInserts))

            }

            planRep.createPlanTemp(
                generatePlanModel(request),
                planProducts,
                planProcesses,
                planChildrenProcesses,
                planDetails
            )
        } catch (e: Exception) {
            throw e
        } finally {
            systemLockRep.unlock(typeOfSystemLocks)
        }
    }

    private fun generatePlanModel(request: CreatePlanRequest): PlanTemp {
        return PlanTemp(
            planCode = "F${DateTimeHelper.toString(request.startDate!!, DateTimeFormat.yyyyMMdd)}T${DateTimeHelper.toString(request.endDate!!, DateTimeFormat.yyyyMMdd)}",
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            version = 0,
            isActive = true
        )
    }

    private fun generatePlanProductModel(product: Product): PlanProductTemp {
        return PlanProductTemp(
            productName = product.name,
            frame_1 = product.frame_1,
            mold = product.mold,
            pcsSh = product.pcsSh,
            blockSh = product.shBlock,
        )
    }

    private fun generatePlanProcessModel(process: ProductProcessModel, completionRate: BigDecimal?, inventory: Int?): PlanProcessTemp {
        return PlanProcessTemp(
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

    private fun generatePlanDetailINSModel(orderInfo: List<OrderInfo>, unit: String, blockSh: Int): List<PlanDetailTemp> {
        val data = mutableListOf<PlanDetailTemp>()
        var accumulation = 0
        for (order in orderInfo.sortedBy { x -> x.orderDate }) {
            when (unit) {
                ProcessUnit.SHEET -> {
                    data.add(PlanDetailTemp(
                        title = PlanTitle.PLAN_KEY,
                        planDate = order.orderDate,
                        sheetQuantity = order.quantity ?: 0,
                        blockQuantity = (order.quantity ?: 0) * blockSh
                    ))
                    data.add(PlanDetailTemp(
                        title = PlanTitle.PLAN_ACCUMULATION_KEY,
                        planDate = order.orderDate,
                        sheetQuantity = (order.quantity ?: 0) + accumulation,
                        blockQuantity = ((order.quantity ?: 0) + accumulation) * blockSh
                    ))
                    accumulation += (order.quantity ?: 0)
                }

                ProcessUnit.BLOCK -> {
                    data.add(PlanDetailTemp(
                        title = PlanTitle.PLAN_KEY,
                        planDate = order.orderDate,
                        sheetQuantity = (order.quantity ?: 0) / blockSh,
                        blockQuantity = order.quantity ?: 0
                    ))
                    data.add(PlanDetailTemp(
                        title = PlanTitle.PLAN_ACCUMULATION_KEY,
                        planDate = order.orderDate,
                        sheetQuantity = ((order.quantity ?: 0) + accumulation) / blockSh,
                        blockQuantity = (order.quantity ?: 0) + accumulation
                    ))
                    accumulation += (order.quantity ?: 0)
                }
            }
        }
        return data
    }

    private fun generatePlanDetailModel(currentPlanDetail: List<PlanDetailTemp>, unit: String, blockSh: Int): List<PlanDetailTemp> {
        val data = mutableListOf<PlanDetailTemp>()
        var accumulation = 0

        return data
    }


    private fun sortProduct(
        orderInfo: List<OrderInfo>,
        productInfo: List<Product>,
        inventories: List<InventoryProductResponse>,
        productProcesses: List<ProductProcessModel>
    ): List<String> {
        val data = mutableListOf<String>()

        val orderByDateGroup = orderInfo.groupBy { x -> x.orderDate }.toSortedMap(compareBy { it })
        for (orderGrp in orderByDateGroup) {
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
        }


        return data
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
}