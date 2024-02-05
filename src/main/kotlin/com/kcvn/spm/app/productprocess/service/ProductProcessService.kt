package com.kcvn.spm.sample.service

import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
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
    fun  getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): BasePagingResponse<ProductProcessResponse>
    {
        val result = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode,pageable);
        var response = BasePagingResponse<ProductProcessResponse>();
        val listProductName = result.first.map { it.productName }
        val  uniqueListProductName = listProductName.distinct()
        val listProduct = productRepository.getByName(uniqueListProductName.filterNotNull())
            response.data = result.first.map { productProcess ->
                ProductProcessResponse(
                    id = productProcess.id,
                    productName = productProcess.productName,
                    layerCode = productProcess.layerCode,
                    processCode = productProcess.processCode,
                    processName = productProcess.processName,
                    processNameJp = productProcess.processNameJp,
                    processConvertCode = productProcess.processConvertCode,
                    processStatisticCode = productProcess.processStatisticCode,
                    processInventoryCode = productProcess.processInventoryCode,
                    productId = listProduct.find { x -> x.name == productProcess.productName }?.id
                );
            }
            response.totalRecords = result.second;

        return response;
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

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest) : List<ProductProcess?> {
        val dataResult: MutableList<ProductProcess?> = mutableListOf()
        for (item in request.listProcess!!){
            val productProcess = productProcessRep.getByProductProcessDetailById(item.id)
                ?: throw BusinessException(CommonUtils.getMessage("productProcess.notFound"))
            productProcess.processConvertCode = item.processConvertCode;
            productProcess.processStatisticCode = item.processStatisticCode;
            productProcess.processInventoryCode = item.processInventoryCode;

            val data = productProcessRep.updateProductDetail(productProcess);
            dataResult.add(data)
        }
        return  dataResult
    }
}