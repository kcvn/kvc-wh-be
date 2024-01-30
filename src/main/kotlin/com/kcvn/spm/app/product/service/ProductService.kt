package com.kcvn.spm.app.product.service

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
////
@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val productProcessRep: ProductProcessRepository,
    private val completionRateProductRep: CompletionRateProductRepository
) {

    fun getListProduct(request: ProductSearchRequest?, pageable: Pageable) : PagingProductResponse {
        val products = productRep.getList(request, pageable)
        val response = PagingProductResponse()

        if (products.first.isNotEmpty()) {
            val productNames = products.first.mapNotNull { x -> x.name }
            val completionRates = completionRateProductRep.getByProduct(productNames)
            val productProcesses = productProcessRep.getByProduct(productNames)
            val processGroups = productProcesses.groupBy { x -> Pair(x.productName, x.processCode) }

            response.data = products.first.map { x -> ProductResponse(
                id = x.id,
                name = x.name,
                exportType = x.exportType,
                size = x.size,
                frame_1 = x.frame_1,
                frame_2 = x.frame_2,
                mold = x.mold,
                productLine = x.productLine,
                srNosr = x.srNosr,
                pcsSh = x.pcsSh,
                shBlock = x.shBlock,
                layerCount = x.layerCount,
                ringJig = x.ringJig,
                snapMold = x.snapMold,
                tapeCommon = x.tapeCommon,
                tapeType = x.tapeType,
                completionRate = (completionRates.find { m -> m.productName == x.name }?.rate ?: 0.0).toDouble(),
                lstProcess = processGroups.filter { m -> m.key.first == x.name }.mapNotNull { m -> DropdownResponse(m.key.second, m.value.size.toString()) }
            ) }
            response.totalRecords = products.second
            response.collumns = productProcesses.mapNotNull { x -> DropdownResponse(x.processCode,x.processName) }.distinct()
        }

        return response
    }
}