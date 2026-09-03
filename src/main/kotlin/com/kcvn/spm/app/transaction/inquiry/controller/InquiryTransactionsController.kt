package com.kcvn.spm.app.transaction.inquiry.controller

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.app.transaction.inquiry.payload.request.InquirySearchRequest
import com.kcvn.spm.app.transaction.inquiry.payload.response.InquiryResponse
import com.kcvn.spm.app.transaction.inquiry.service.InquiryTransactionsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inquiry")
class InquiryTransactionsController(private val inquiryService: InquiryTransactionsService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun getList(
        request: InquirySearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<InquiryResponse>> {
        val result = inquiryService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }
}