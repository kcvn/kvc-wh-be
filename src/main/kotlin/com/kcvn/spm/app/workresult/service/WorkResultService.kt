package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.repository.WorkResultRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WorkResultService (
    private val workResultRep: WorkResultRepository
) {
    fun getListProductResult(request: WorkResultSearchRequest?, pageable: Pageable): PagingWorkResultResponse {
        val workResults = workResultRep.getListWorkResult(request,pageable)
        val response = PagingWorkResultResponse()




        return response
    }
}