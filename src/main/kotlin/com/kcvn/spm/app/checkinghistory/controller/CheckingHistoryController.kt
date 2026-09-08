package com.kcvn.spm.app.checkinghistory.controller

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.app.checkinghistory.service.CheckingHistoryService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/checking-history")
class CheckingHistoryController(private val checkingHistoryService: CheckingHistoryService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun getList(
        request: CheckingHistorySearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["updatedDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<CheckingHistoryResponse>> {
        val result = checkingHistoryService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: List<CheckingHistoryRequest>): ResponseEntity<*> {
        checkingHistoryService.save(request)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post checking-history/create" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }

    @GetMapping("/export-csv")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun exportCsv(
        request: CheckingHistorySearchRequest,
        response: HttpServletResponse,
    ) {
        response.contentType = "text/csv"
        val fileName = "receive_checking_result_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.csv"
        response.setHeader("Content-Disposition", "attachment; filename=$fileName")
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition")

        val pagingResponse: BasePagingResponse<CheckingHistoryResponse> =
            checkingHistoryService.getList(request, Pageable.unpaged())

        val productsList: List<CheckingHistoryResponse> = pagingResponse.data ?: emptyList()

        val writer = response.writer
        writer.append("scanned_date,form_code,po_no,imported_qty,scanned_qty,seq_no,result\n")
        productsList.forEach { item ->
            writer.append("${item.scannedDate},${item.invoiceNo},${item.poNumber},${item.orderQty},${item.scanQty},${item.seqNo},${item.result}\n")
        }
        writer.flush()
    }
}