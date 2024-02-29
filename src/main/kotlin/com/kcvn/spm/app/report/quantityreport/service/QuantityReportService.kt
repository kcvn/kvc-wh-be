package com.kcvn.spm.app.report.quantityreport.service

import com.kcvn.spm.app.completionrate.service.CompletionRateService
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.app.productprocess.service.ProductProcessService
import com.kcvn.spm.app.report.quantityreport.payload.model.CalculateQuantityOfProcessRequest
import com.kcvn.spm.app.report.quantityreport.payload.model.ProductOrderDateKey
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.CalculateQuantityReportRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuantityReportService(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val productProcessService: ProductProcessService,
    private val completionRateService: CompletionRateService,
    private val calculateQuantityReportRep: CalculateQuantityReportRepository,
) {
    fun calculateQuantity(request: CalculateQuantityRequest): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findByMonthReport(request)
        return if (calculateQuantityReport != null && calculateQuantityReport.status == true) {
            BaseResponse(data = false, message = CommonUtils.getMessage("calculated.locked.error"))
        } else {
            val order = orderService.getOrderCodeByMonth(request)
            val listCalculateQuantityOfProcessWithoutCheckVersion = mutableListOf<CalculateQuantityOfProcessRequest>()
            val listCalculateQuantityProcess = mutableListOf<CalculateQuantityOfProcessRequest>()
            order.forEach { y ->
                val orderDetails = orderService.getOrderDetailsByOrderId(y.id)
                orderDetails.forEach { x ->
                    run {
                        val product = productService.getProductDetailWithCompletionRateById(x.productId)
                        if (product != null) {
                            listCalculateQuantityOfProcessWithoutCheckVersion.add(
                                CalculateQuantityOfProcessRequest(
                                    version = y.version,
                                    productName = product.name,
                                    blockSh = product.shBlock,
                                    quantity = x.quantity,
                                    completionRate = product.rate,
                                    orderDate = x.orderDate,
                                    effectiveDate = product.effectiveDate,
                                    expirationDate = product.expirationDate,
                                )
                            )
                        }
                    }
                }
            }
            val groupedData = listCalculateQuantityOfProcessWithoutCheckVersion.groupBy {
                ProductOrderDateKey(
                    it.productName,
                    it.orderDate
                )
            }
            val result = groupedData.mapValues { (_, value) -> value.maxByOrNull { it.version ?: 0 } }
            result.forEach { x ->
                run {
                    listCalculateQuantityProcess.add(
                        CalculateQuantityOfProcessRequest(
                            version = x.value?.version,
                            productName = x.value?.productName,
                            blockSh = x.value?.blockSh,
                            quantity = x.value?.quantity,
                            completionRate = x.value?.completionRate,
                            orderDate = x.value?.orderDate,
                            effectiveDate = x.value?.effectiveDate,
                            expirationDate = x.value?.expirationDate,
                        )
                    )
                }
            }
            listCalculateQuantityProcess.forEach { x ->
                run {
                    if (x.orderDate?.isBefore(x.effectiveDate) == true) {
                        throw BusinessException("Sản phẩm ${x.productName}, ngày đặt hàng ${x.orderDate.toLocalDate()} không xác định được thông tin tỷ lệ đạt!!!")
                    }
                }
            }

            println(listCalculateQuantityProcess)

            BaseResponse(data = true, message = CommonUtils.getMessage("calculated.success"))
        }
    }

    fun lockedQuantity(request: String): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findById(request)
        calculateQuantityReportRep.update(calculateQuantityReport)
        return BaseResponse(true, message = CommonUtils.getMessage("quantity.locked.success"))
    }

}