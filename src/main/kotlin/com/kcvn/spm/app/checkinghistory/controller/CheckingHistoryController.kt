package com.kcvn.spm.app.checkinghistory.controller

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryExportResponse
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.app.checkinghistory.service.CheckingHistoryService
import com.kcvn.spm.app.tempcheckingimported.payload.response.TempCheckingImportedResponse
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsvBuilder
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

@RestController
@RequestMapping("/api/checking-history")
class CheckingHistoryController(private val checkingHistoryService: CheckingHistoryService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
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
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
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
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun exportCsv(
        request: CheckingHistorySearchRequest,
        response: HttpServletResponse,
    ) {
        response.contentType = "text/csv"
        response.setHeader("Content-Disposition", "attachment; filename=\"products.csv\"")

        val pagingResponse: BasePagingResponse<CheckingHistoryResponse> =
            checkingHistoryService.getList(request, Pageable.unpaged())

        val productsList: List<CheckingHistoryResponse> = pagingResponse.data ?: emptyList()

        val exportList = productsList.map { item ->
            CheckingHistoryExportResponse(
                scanDate = item.scanDate,
                formCode = item.formCode,
                poNumber = item.poNumber,
                importQty = item.importQty,
                scanQty = item.scanQty,
                seqNo = item.seqNo,
                result = item.result
            )
        }

        val writer = response.writer
        val beanToCsv = StatefulBeanToCsvBuilder<CheckingHistoryExportResponse>(writer)
            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
            .build()

        beanToCsv.write(exportList)
        writer.flush()
    }
}