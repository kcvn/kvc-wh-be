package com.kcvn.spm.sample.service

import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.ProductProcessRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductProcessService(
    private val productProcessRep : ProductProcessRepository
)
{
    fun getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): PaginatedResponse
    {
        val result = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode,pageable);
        return PaginatedResponse(result.first.map {
            productProcess -> ProductProcessResponse(
                id = productProcess.id,
                productName = productProcess.productName,
                layerCode = productProcess.layerCode,
                processCode = productProcess.processCode,
                processName = productProcess.processName,
                processNameJp = productProcess.processNameJp,
                processConvertCode = productProcess.processConvertCode,
                processStatisticCode = productProcess.processStatisticCode,
                processInventoryCode = productProcess.processInventoryCode
            )
        }, result.second)
    }

    fun getProductProcessDetail(nameProduct: String?) : List<ProductProcessResponse?>?{
        val result = productProcessRep.getByProductProcessDetail(nameProduct)
        return result?.map {
            productProcess ->
            ProductProcessResponse(
                id = productProcess?.id,
                productName = productProcess?.productName,
                layerCode = productProcess?.layerCode,
                processCode = productProcess?.processCode,
                processName = productProcess?.processName,
                processNameJp = productProcess?.processNameJp,
                processConvertCode = productProcess?.processConvertCode,
                processStatisticCode = productProcess?.processStatisticCode,
                processInventoryCode = productProcess?.processInventoryCode

            )
        }
    }
}