package com.kcvn.spm.app.masterData.service

import com.kcvn.spm.app.masterData.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.repository.CommonCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MasterDataService(
    private val commonCategoryRep: CommonCategoryRepository
) {
    fun getMasterDataSelection() : MasterDataSelectionResponse {
        val types = listOf(
            Constants.KHUNG_1, Constants.KHUNG_2, Constants.KHUON_DUC,
            Constants.SR_OR_NSR, Constants.LOAI_XUAT_HANG, Constants.LOAI_TAPE
        )
        val data = commonCategoryRep.getByType(types)
        return MasterDataSelectionResponse(
            frame1Selections = data.filter { x -> x.type == Constants.KHUNG_1 }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            frame2Selections = data.filter { x -> x.type == Constants.KHUNG_2 }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            moldSelections = data.filter { x -> x.type == Constants.KHUON_DUC }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            srNosrSelections = data.filter { x -> x.type == Constants.SR_OR_NSR }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            exportTypeSelections = data.filter { x -> x.type == Constants.LOAI_XUAT_HANG }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            tapeTypeSelections = data.filter { x -> x.type == Constants.LOAI_TAPE }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
        )
    }
}