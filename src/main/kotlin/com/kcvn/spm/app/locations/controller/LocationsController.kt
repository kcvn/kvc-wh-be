package com.kcvn.spm.app.locations.controller

import com.kcvn.spm.app.locations.payload.request.LocationsRequest
import com.kcvn.spm.app.locations.service.LocationsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/locations")
class LocationsController(private val locationsService: LocationsService) {
    @GetMapping("get-list-location-for-dropdown")
    fun getListLocationDropdown(): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val data = locationsService.getListLocationDropdown()
        return ResponseEntity<BaseResponse<List<DropdownResponse>>>(data, HttpStatus.OK)
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getAllLocations(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE) pageable: Pageable?
    ): ResponseEntity<*> {
        val result = locationsService.findAllPaginated(search, pageable!!)
        return if (result.data.isEmpty())
            ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
        else
            ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createLocation(@Valid @RequestBody request: LocationsRequest?): ResponseEntity<*> {
        val location = locationsService.createLocation(request!!)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post locations/create" + ", REQUEST: " + request
        )
        return if (location == null) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.failed")),
                HttpStatus.BAD_REQUEST
            )
        } else {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded"), location),
                HttpStatus.CREATED
            )
        }
    }
}