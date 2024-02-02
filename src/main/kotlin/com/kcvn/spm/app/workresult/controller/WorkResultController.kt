package com.kcvn.spm.app.workresult.controller

import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.app.workresult.service.WorkResultService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/work-result")
class WorkResultController (
    private val workResultService: WorkResultService
) {
    @GetMapping("/get-list")
    fun getList(
        request: WorkResultSearchRequest?,
        @PageableDefault(size = 10, page = 0, sort = ["summary_result_date,desc","item_name,asc","layer_code,asc","process_name,asc"]) pageable: Pageable
    ): ResponseEntity<PagingWorkResultResponse> {
        return try {
            val data = workResultService.getListProductResult(request,pageable)
            ResponseEntity<PagingWorkResultResponse>(data, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<PagingWorkResultResponse>(null, HttpStatus.OK)
        }
    }

}