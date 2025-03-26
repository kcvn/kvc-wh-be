package com.kcvn.spm.app.westfactory.service

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.westfactory.payload.response.WestFactoryResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.LayoutDetailResponse
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.WestFactoryRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WestFactoryService(
    private val westFactoryRepo: WestFactoryRepository,
    private val backlogWhRepo: BacklogWhRepository,
) {
    fun getList(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<WestFactoryResponse> {
        val westFactoryData = westFactoryRepo.getList(request, pageable)
        val backlogData = backlogWhRepo.getListWithQtyGtZero()
        val locationCodes = backlogData.map { backlog -> backlog.locationCode }
        val data = westFactoryData.first.map {
            WestFactoryResponse(
                column1 = getLayoutDetailResponse(it.column1, locationCodes),
                column2 = getLayoutDetailResponse(it.column2, locationCodes),
                column3 = getLayoutDetailResponse(it.column3, locationCodes),
                column4 = getLayoutDetailResponse(it.column4, locationCodes),
                column5 = getLayoutDetailResponse(it.column5, locationCodes),
                column6 = getLayoutDetailResponse(it.column6, locationCodes),
                column7 = getLayoutDetailResponse(it.column7, locationCodes),
                column8 = getLayoutDetailResponse(it.column8, locationCodes),
                column9 = getLayoutDetailResponse(it.column9, locationCodes),
                column10 = getLayoutDetailResponse(it.column10, locationCodes),
                column11 = getLayoutDetailResponse(it.column11, locationCodes),
                column12 = getLayoutDetailResponse(it.column12, locationCodes),
                column13 = getLayoutDetailResponse(it.column13, locationCodes),
                column14 = getLayoutDetailResponse(it.column14, locationCodes),
                column15 = getLayoutDetailResponse(it.column15, locationCodes),
                column16 = getLayoutDetailResponse(it.column16, locationCodes),
                column17 = getLayoutDetailResponse(it.column17, locationCodes),
                column18 = getLayoutDetailResponse(it.column18, locationCodes),
                column19 = getLayoutDetailResponse(it.column19, locationCodes),
                column20 = getLayoutDetailResponse(it.column20, locationCodes),
                column21 = getLayoutDetailResponse(it.column21, locationCodes),
                column22 = getLayoutDetailResponse(it.column22, locationCodes),
                column23 = getLayoutDetailResponse(it.column23, locationCodes),
                column24 = getLayoutDetailResponse(it.column24, locationCodes),
                column25 = getLayoutDetailResponse(it.column25, locationCodes),
                column26 = getLayoutDetailResponse(it.column26, locationCodes),
                column27 = getLayoutDetailResponse(it.column27, locationCodes),
                column28 = getLayoutDetailResponse(it.column28, locationCodes),
                column29 = getLayoutDetailResponse(it.column29, locationCodes),
                column30 = getLayoutDetailResponse(it.column30, locationCodes),
                column31 = getLayoutDetailResponse(it.column31, locationCodes),
                column32 = getLayoutDetailResponse(it.column32, locationCodes),
                column33 = getLayoutDetailResponse(it.column33, locationCodes),
                column34 = getLayoutDetailResponse(it.column34, locationCodes),
                column35 = getLayoutDetailResponse(it.column35, locationCodes)
            )
        }
        return BasePagingResponse(
            data,
            westFactoryData.second
        )
    }

    fun getLayoutDetailResponse(value: String?, locationCodes: List<String?>): LayoutDetailResponse {
        return if (value == "E") {
            LayoutDetailResponse(value = value, color = null)
        } else {
            if (locationCodes.contains(value)) {
                // có tồn kho màu đỏ
                LayoutDetailResponse(value = value, color = "#FF0000")
            } else {
                // không tồn kho màu xanh
                LayoutDetailResponse(value = value, color = "#0000FF")
            }
        }
    }
}