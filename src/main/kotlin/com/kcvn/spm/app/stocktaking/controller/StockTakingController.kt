package com.kcvn.spm.app.stocktaking.controller

import com.kcvn.spm.app.stocktaking.payload.request.*
import com.kcvn.spm.app.stocktaking.payload.response.ActualStockTakingResponse
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingForAndroid
import com.kcvn.spm.app.stocktaking.payload.response.SystemStockTakingResponse
import com.kcvn.spm.app.stocktaking.service.StockTakingService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
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
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/stock-taking")
class StockTakingController(private val stockTakingService: StockTakingService) {
    @GetMapping("/system-stock-taking")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getListSystemStock(
        request: StockTakingDailyRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["locationCode"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<SystemStockTakingResponse>> {
        val result = stockTakingService.getListSystemStock(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/actual-stock-taking")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getListActualStock(
        request: StockTakingMonthlyRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["locationCode"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<ActualStockTakingResponse>> {
        val result = stockTakingService.getListActualStock(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/actual-stock-taking/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getListForAndroid(
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["inspectionDate"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<StockTakingForAndroid>> {
        val result = stockTakingService.getListForAndroid(pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/actual-stock-taking/checking-start")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun checkingStartActual(request: StartActualRequest): ResponseEntity<*> {
        val response = stockTakingService.checkingStartActual(request)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post actual-stock-taking/checking-start" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(message = response.message!!, data = response.data),
            HttpStatus.CREATED
        )
    }

    @PostMapping("/actual-stock-taking/start")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun startActual(@Valid @RequestBody request: StartActualRequest): ResponseEntity<*> {
        val response = stockTakingService.startActual(request)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post actual-stock-taking/start" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(message = response.message!!, data = response.data),
            HttpStatus.CREATED
        )
    }

    @PostMapping("/actual-stock-taking/scan")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun scan(@Valid @RequestBody request: List<ScanRequest>): ResponseEntity<*> {
        stockTakingService.scan(request)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post actual-stock-taking/scan" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }

    @PostMapping("/actual-stock-taking/stop")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun stopActual(@Valid @RequestBody request: StopActualRequest): ResponseEntity<*> {
        stockTakingService.stopActual(request)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post actual-stock-taking/stop" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importExcelAmoeba(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = stockTakingService.importExcelAmoeba(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}