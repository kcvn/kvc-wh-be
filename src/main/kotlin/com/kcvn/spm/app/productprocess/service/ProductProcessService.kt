package com.kcvn.spm.sample.service

import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductProcessService(
    private val productProcessRep : ProductProcessRepository,
    private val productRepository: ProductRepository
)
{
    fun getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): PaginatedResponse
    {
        val result = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode,pageable);
        val listProductName = result.first.map { it.productName }
        val  uniqueListProductName = listProductName.distinct()
        val listProduct = productRepository.getProductByListName(uniqueListProductName);
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
                processInventoryCode = productProcess.processInventoryCode,
                productId = listProduct?.find { x -> x.name == productProcess.productName }?.id
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

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest) : List<ProductProcessResponse>? {
        var dataResult: MutableList<ProductProcessResponse> = mutableListOf()
        for (item in request.listProcess!!){
            if(item.processInventoryCode == null){
                throw BusinessException(CommonUtils.getMessage("Process Inventory Code Not Null"))
            }
            if(item.processStatisticCode == null){
                throw BusinessException(CommonUtils.getMessage("Process Statistic Code Not Null"))
            }

            val productProcess = productProcessRep.getByProductProcessDetailById(item.id)
            if(productProcess == null){
                throw BusinessException(CommonUtils.getMessage("Product Process Not Found "))
            }
            productProcess.processConvertCode = item.processConvertCode;
            productProcess.processStatisticCode = item.processStatisticCode;
            productProcess.processInventoryCode = item.processInventoryCode;

            val data = productProcessRep.updateProductDetail(productProcess);
            val result = ProductProcessResponse(
                id = data?.id,
                productName = data?.productName,
                layerCode = data?.layerCode,
                processCode = data?.processCode,
                processName = data?.processName,
                processNameJp = data?.processNameJp,
                processConvertCode = data?.processConvertCode,
                processStatisticCode = data?.processStatisticCode,
                processInventoryCode = data?.processInventoryCode
            )
            dataResult.add(result)
        }
        return  dataResult
    }
}