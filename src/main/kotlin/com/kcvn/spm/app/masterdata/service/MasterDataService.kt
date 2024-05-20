package com.kcvn.spm.app.masterdata.service

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.common.constants.MasterDataType
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
            MasterDataType.KHUNG_1, MasterDataType.KHUNG_2, MasterDataType.KHUON_DUC,
            MasterDataType.SR_OR_NSR, MasterDataType.LOAI_XUAT_HANG, MasterDataType.LOAI_TAPE,
            MasterDataType.MACHUYENDOI, MasterDataType.MATHONGKE, MasterDataType.TAPE_DUNG_CHUNG, MasterDataType.RING_JIG
        )
        val data = commonCategoryRep.getByType(types)
        return MasterDataSelectionResponse(
            frame1Selections = data.filter { x -> x.type == MasterDataType.KHUNG_1 }.map { x -> DropdownResponse(x.value, x.value) },
            frame2Selections = data.filter { x -> x.type == MasterDataType.KHUNG_2 }.map { x -> DropdownResponse(x.value, x.value) },
            moldSelections = data.filter { x -> x.type == MasterDataType.KHUON_DUC }.map { x -> DropdownResponse(x.value, x.value) },
            srNosrSelections = data.filter { x -> x.type == MasterDataType.SR_OR_NSR }.map { x -> DropdownResponse(x.value, x.value) },
            exportTypeSelections = data.filter { x -> x.type == MasterDataType.LOAI_XUAT_HANG }.map { x -> DropdownResponse(x.value, x.value) },
            tapeTypeSelections = data.filter { x -> x.type == MasterDataType.LOAI_TAPE }
                .sortedBy { it.value }.map { x -> DropdownResponse(x.value, x.value) },
            processConvertCodes = data.filter { x -> x.type == MasterDataType.MACHUYENDOI }.map { x -> DropdownResponse(x.value, x.value) },
            processStatisticCodes = data.filter { x -> x.type == MasterDataType.MATHONGKE }.map { x -> DropdownResponse(x.value, x.value) },
            tapeCommonSelections = data.filter { x -> x.type == MasterDataType.TAPE_DUNG_CHUNG }
                .sortedBy { it.value }.map { x -> DropdownResponse(x.value, x.value) },
            ringJigSelections = data.filter { x -> x.type == MasterDataType.RING_JIG }.map { x -> DropdownResponse(x.value, x.value) }
        )
    }



}