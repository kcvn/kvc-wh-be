package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.app.workresult.payload.response.ProcessGroupResponse
import com.kcvn.spm.app.workresult.payload.response.ProcessResponse
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.WorkResultRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WorkResultService (
    private val workResultRep: WorkResultRepository
) {
    fun getListWorkResult(request: WorkResultSearchRequest?, pageable: Pageable): BasePagingResponse<WorkResultResponse> {
        val workResults = workResultRep.getPagingListWorkResult(request,pageable)
        var response = BasePagingResponse<WorkResultResponse>()

        if (workResults.first.isNotEmpty()) {
            response = mappingWorkResultResponse(workResults.first)
            response.totalRecords = workResults.second
        }


        return response
    }

    private fun mappingWorkResultResponse(workResults: List<WorkResult>): BasePagingResponse<WorkResultResponse> {
        val response = PagingWorkResultResponse()
        response.data = workResults.map { x -> WorkResultResponse(
            id = x.id,
            summaryResultDate = x.summaryResultDate,
            itemName = x.itemName,
            processName = x.processName,
            processCode = x.processCode,
            layerCode = x.layerCode,
            totalTapeQuantity = x.totalTapeQuantity,
            totalSheetQuantity = x.totalSheetQuantity,
            goodTapeQuantity = x.goodTapeQuantity,
            goodSheetQuantity = x.goodSheetQuantity,
            orderCode = x.orderCode,
            tapeLotNo = x.tapeLotNo,
            code = x.code,
            workImplementBy = x.workImplementBy,
            equipmentName = x.equipmentName
        )
        }

        return response
    }

    fun getListProcessGroup(): List<ProcessGroupResponse> {
        return workResultRep.getListProcessGroup()
    }


    fun getListProcessByGroupCode(groupCode: Array<String>): List<ProcessResponse> {
        return workResultRep.getListProcessByGroupCode(groupCode)
    }

}