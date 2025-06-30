package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.repository.CheckingHistoryRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class CheckingHistoryService(
    private val checkingHistoryRepo: CheckingHistoryRepository
) {
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable): BasePagingResponse<CheckingHistoryResponse> {
        val checkingHistoryData = checkingHistoryRepo.getList(request, pageable)
        val data = checkingHistoryData.first.map {
            CheckingHistoryResponse(
                scanDate = it.scanDate,
                poNumber = it.poNumber,
                importQty = it.importQty,
                scanQty = it.scanQty,
                seqNo = it.seqNo,
                formCode = it.formCode,
                result = getResult(it)
            )
        }
        return BasePagingResponse(
            data,
            checkingHistoryData.second
        )
    }

    private fun getResult(data: CheckingHistory): String {
        return when {
            data.importQty!! < data.scanQty -> "Thừa"
            data.importQty == data.scanQty -> "Đủ"
            else -> "Thiếu"
        }
    }

    fun save(requestList: List<CheckingHistoryRequest>) {
        val scanDate = LocalDate.now()
        requestList.forEach { request ->
            if (!request.reChecking) {
                val data = checkingHistoryRepo.findByScanDateAndPOAndSeqNoAndFormCode(scanDate, request.poNumber, 1, request.formCode)
                if (data == null) {
                    val domain = CheckingHistory(
                        poNumber = request.poNumber,
                        importQty = request.importQty,
                        scanQty = request.scanQty,
                        seqNo = 1,
                        formCode = request.formCode
                    )
                    checkingHistoryRepo.save(domain)
                } else {
                    val newScanQty = data.scanQty?.plus(request.scanQty)
                    checkingHistoryRepo.update(newScanQty!!, scanDate, data.poNumber!!, 1, data.formCode!!)
                }
            } else {
                val data = checkingHistoryRepo.findLatestByScanDateAndPOAndFormCode(scanDate, request.poNumber, request.formCode)
                    ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.checkingHistory"), "scanDate = ${scanDate}, poNumber = ${request.poNumber}")
                val seqNo = data.seqNo?.plus(1)
                val domain = CheckingHistory(
                    poNumber = request.poNumber,
                    importQty = request.importQty,
                    scanQty = request.scanQty,
                    seqNo = seqNo,
                    formCode = request.formCode
                )
                checkingHistoryRepo.save(domain)
            }
        }
    }
}