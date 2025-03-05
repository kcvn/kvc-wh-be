package com.kcvn.spm.app.transaction.sending.controller

import com.kcvn.spm.app.transaction.sending.payload.request.MovingRequest
import com.kcvn.spm.app.transaction.sending.payload.request.MovingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.request.SendingRequest
import com.kcvn.spm.app.transaction.sending.payload.response.MovingResponse
import com.kcvn.spm.app.transaction.sending.service.SendingTransactionsService
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
@RequestMapping("/api/sending")
class SendingTransactionsController(private val sendingService: SendingTransactionsService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: MovingSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["sourceLocationCode"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<MovingResponse>> {
        val result = sendingService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping("/create/moving")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createMoving(@Valid @RequestBody request: List<MovingRequest>?): ResponseEntity<*> {
        val response = sendingService.validateSourceBacklogFromMoving(request!!)

        return if (response.isNotEmpty()) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("Không đủ tồn kho"), response),
                HttpStatus.OK
            )
        } else {
            sendingService.saveMoving(request)
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded")),
                HttpStatus.CREATED
            )
        }
    }

    @PostMapping("/create/sending-trans")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createSendTrans(@Valid @RequestBody request: List<SendingRequest>?): ResponseEntity<*> {
        val response = sendingService.validateSourceBacklogFromSending(request!!)

        return if (response.isNotEmpty()) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("Không đủ tồn kho"), response),
                HttpStatus.OK
            )
        } else {
            sendingService.saveSendTrans(request)
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded")),
                HttpStatus.CREATED
            )
        }
    }
}