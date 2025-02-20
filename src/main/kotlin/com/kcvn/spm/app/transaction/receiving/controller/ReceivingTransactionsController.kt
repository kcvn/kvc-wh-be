package com.kcvn.spm.app.transaction.receiving.controller

import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransSearchRequest
import com.kcvn.spm.app.transaction.receiving.payload.response.RecTransResponse
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/receiving")
class ReceivingTransactionsController(private val receivingService: ReceivingTransactionsService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: RecTransSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["destLocationCode"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<RecTransResponse>> {
        val result = receivingService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createRecTrans(@Valid @RequestBody request: List<RecTransRequest>?): ResponseEntity<*> {
        receivingService.saveRecTrans(request!!)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }
}