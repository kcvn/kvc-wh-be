package com.kcvn.spm.app.transaction.receiving.controller

import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransSearchRequest
import com.kcvn.spm.app.transaction.receiving.payload.response.RecTransResponse
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/receiving")
class ReceivingTransactionsController(private val receivingService: ReceivingTransactionsService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun getList(
        request: RecTransSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<RecTransResponse>> {
        val result = receivingService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }
}