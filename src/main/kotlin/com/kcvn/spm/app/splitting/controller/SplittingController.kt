package com.kcvn.spm.app.splitting.controller

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.payload.request.TempBacklogWhSearchRequest
import com.kcvn.spm.app.splitting.payload.respone.TempBacklogWhResponse
import com.kcvn.spm.app.splitting.payload.respone.TempSplittingResponse
import com.kcvn.spm.app.splitting.service.SplittingService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/splitting")
class SplittingController(private val splittingService: SplittingService) {

    @GetMapping("/temp/get-package-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INVENTORY.value) || hasRole('ADMIN')")
    fun getTempPackageList(
        request: TempBacklogWhSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<TempSplittingResponse>> {
        val result = splittingService.getTempPackageList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/temp/get-package-detail")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INVENTORY.value) || hasRole('ADMIN')")
    fun getTempPackageDetail(
        @RequestParam tempSplittingId: String,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<TempBacklogWhResponse>> {
        val result = splittingService.getTempPackageDetail(tempSplittingId, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @DeleteMapping("/temp/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun deleteTempPackage(@PathVariable("id") id: String): ResponseEntity<*> {
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: delete splitting/temp/delete" + ", REQUEST: " + id
        )
        splittingService.deleteTempPackage(id)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.OK
        )
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun createSplitting(@Valid @RequestBody request: List<SplittingRequest>?): ResponseEntity<*> {
        val data = splittingService.saveSplitting(request!!)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post splitting/create" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded"), data),
            HttpStatus.CREATED
        )
    }
}