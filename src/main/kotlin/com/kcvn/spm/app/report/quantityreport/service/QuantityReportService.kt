package com.kcvn.spm.app.report.quantityreport.service

import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.product.payload.model.ProcessGroupModel
import com.kcvn.spm.app.report.quantityreport.payload.model.*
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityOfProcessRequest
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantityDetail
import com.kcvn.spm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime


@Service
@Transactional
class QuantityReportService(
    private val orderService: OrderService,
    private val productRep: ProductRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val productProcessRep: ProductProcessRepository,
    private val processGroupRep: ProcessGroupRepository,
    private val calculateQuantityReportRep: CalculateQuantityReportRepository,
) {
    fun calculateQuantity(request: CalculateQuantityRequest): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findByMonthReport(request)
        return if (calculateQuantityReport != null && calculateQuantityReport.status == true) {
            BaseResponse(data = false, message = CommonUtils.getMessage("calculated.locked.error"))
        } else {
            val listOrder = orderService.getOrderCodeByMonth(request)
            val listCalculateQuantityProcess = mutableListOf<CalculateQuantityOfProcessRequest>()
            val listOrderDetailError = mutableListOf<ErrorOrderDetail>()
            val orderIds = listOrder.map { x -> x.id }
            val orderDetails = orderService.getOrderDetailsByOrderIds(orderIds)
            val productIDs = orderDetails.map { x -> x.productId }
            val productsWithRate = productRep.getProductDetailWithCompletionRateByIds(productIDs)

            val productNames = productsWithRate.mapNotNull { x -> x?.name }.distinct()
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

            val listCalculateWithoutCheckVersion = orderDetails.map { x ->
                val ord = listOrder.find { m -> m.id == x.orderId }
                val prods = productsWithRate.filter { m -> m?.id == x.productId }

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
                        version = ord?.version,
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
            val groupedData = listCalculateWithoutCheckVersion.groupBy {
                ProductOrderDateKeyModel(
                    it.productName,
                    it.orderDate
                )
            }
            val listCalculateAfterCheckVersion = groupedData.mapValues { (_, value) -> value.maxByOrNull { it.version ?: 0 } }
            val listOrderDetailCalculate = listCalculateAfterCheckVersion.map { x ->
                CalculateQuantityOfProcessRequest(
                    version = x.value?.version,
                    productName = x.value?.productName,
                    blockSh = x.value?.blockSh,
                    quantityBlock = x.value?.quantityBlock,
                    completionRate = x.value?.completionRate,
                    orderDate = x.value?.orderDate,
                    effectiveDate = x.value?.effectiveDate,
                    expirationDate = x.value?.expirationDate,
                )
            }
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
                    quantityProcessStatistic = ((x.processCount!! * x.quantityBlock!!) / (x.blockSh!!.times(x.completionRate!!.toDouble()) / 100)).toInt(),
                    createdDate = OffsetDateTime.now(),
                    createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                )
            }

            val data = listInformationCalculateQuantityDetails.groupBy {
                ProductProcessKeyModel(
                    it.productName,
                    it.processStatisticCode
                )
            }

            val listInformationQuantity = data.map { (key,items)->
                InformationQuantity(
                    monthReport = items.first().monthReport,
                    productName = key.productName,
                    processStatisticCode = key.processStatisticCode,
                    totalQuantityOfProcess = items.sumOf { it.quantityProcessStatistic!! },
                    createdDate = OffsetDateTime.now(),
                    createdBy = CommonUtils.loggedInUser()?: Constants.SYSTEM
                )
            }

            val listCalculateQuantityResult = listOrder.map { x ->
                CalculateQuantityResult(
                    monthReport = x.startDate,
                    startDate = x.startDate,
                    endDate = x.endDate,
                    calculateBy = CommonUtils.loggedInUser()?: Constants.SYSTEM,
                    calculateDate = OffsetDateTime.now(),
                    updatedDate = OffsetDateTime.now(),
                    updatedBy = CommonUtils.loggedInUser()?: Constants.SYSTEM,
                )
            }



            println(listOrderDetailError)
            println(listCalculateQuantityResult)
            println(listInformationQuantity)
            println(listInformationCalculateQuantityDetails)

            BaseResponse(data = true, message = CommonUtils.getMessage("calculated.success"))
        }
    }

    fun lockedQuantity(request: String): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findById(request)
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
                monthReport = x.monthReport,
                startDate = x.startDate,
                endDate = x.endDate,
                status = x.status,
                calculateBy = x.calculateBy,
                calculateDate = x.calculateDate,
                lockedBy = x.lockedBy,
                lockedDate = x.lockedDate
            )
        }
        return response
    }

}