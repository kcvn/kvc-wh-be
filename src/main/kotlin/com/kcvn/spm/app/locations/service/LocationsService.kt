package com.kcvn.spm.app.locations.service

import com.kcvn.spm.app.locations.payload.request.LocationsRequest
import com.kcvn.spm.app.locations.payload.response.LocationsResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Locations
import com.kcvn.spm.repository.LocationsRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LocationsService(private val locationsRepo: LocationsRepository) {
    fun getListLocationDropdown(): BaseResponse<List<DropdownResponse>> {
        val listLocation = locationsRepo.findAll()

        // Map DropDownResponse
        val dropDownList: List<DropdownResponse> = listLocation.map { locationResponse ->
            DropdownResponse(
                locationResponse.locationCode,
                "${locationResponse.locationCode}"
            )
        }

        return BaseResponse(data = dropDownList)
    }

    fun findAllPaginated(search: String?, pageable: Pageable): PaginatedResponse {
        val result = locationsRepo.findByKeywordPaginated(search, pageable)
        return PaginatedResponse(
            result.first.map { LocationsResponse(it.id!!, it.locationCode!!) },
            result.second
        )
    }

    fun createLocation(request: LocationsRequest): LocationsResponse? {
        if (locationsRepo.findByName(request.locationCode!!) != null) {
            throw BusinessException(CommonUtils.getMessage("location.error.codeTaken"))
        }

        val location = Locations(
            null,
            request.locationCode
        )
        val locationId = locationsRepo.save(location)
        return if (locationId != null) {
            LocationsResponse(
                locationId,
                location.locationCode!!
            )
        } else null
    }
}