package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.repository.WorkResultRepository
import org.springframework.data.domain.Pageable

class WorkResultService (
    private val productResultRep: WorkResultRepository
) {
    fun getListProductResult(request: WorkResultSearchRequest?, pageable: Pageable): PagingWorkResultResponse {
        val productResults = productResultRep.getProductResults(request,pageable)
        val response = PagingWorkResultResponse()




        return response
    }
}