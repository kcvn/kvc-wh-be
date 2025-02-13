package com.kcvn.spm.app.backlog.controller

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.PaginatedResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/backlog")
class BacklogController(private val backlogService: BacklogService) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getAllBacklog(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE) pageable: Pageable?
    ): ResponseEntity<*> {
        val result = backlogService.findAllPaginated(search, pageable!!)
        return if (result.data.isEmpty())
            ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
        else
            ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
    }
}