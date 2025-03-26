package com.kcvn.spm.app.westfactory.controller

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.app.westfactory.payload.response.WestFactoryResponse
import com.kcvn.spm.app.westfactory.service.WestFactoryService
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
@RequestMapping("/api/layout/west-factory")
class WestFactoryController(private val westFactoryService: WestFactoryService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: BacklogWhSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["rowNum"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<WestFactoryResponse>> {
        val result = westFactoryService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }
}