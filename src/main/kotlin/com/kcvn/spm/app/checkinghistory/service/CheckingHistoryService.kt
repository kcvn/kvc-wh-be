package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.repository.CheckingHistoryRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional
class CheckingHistoryService(
    private val checkingHistoryRepo: CheckingHistoryRepository
) {
    fun getListFormCodeDropdown(): BaseResponse<List<DropdownResponse>> {
        val listFormCode = checkingHistoryRepo.getListFormCode()

        // Map DropDownResponse
        val dropDownList: List<DropdownResponse> = listFormCode.map { formCode ->
            DropdownResponse(
                formCode,
                formCode
            )
        }

        return BaseResponse(data = dropDownList)
    }

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
        val formCode = getFormCode()
        requestList.forEach { request ->
            if (!request.reChecking) {
                val data = checkingHistoryRepo.findByScanDateAndPOAndSeqNo(scanDate, request.poNumber, 1)
                if (data == null) {
                    val domain = CheckingHistory(
                        poNumber = request.poNumber,
                        importQty = request.importQty,
                        scanQty = request.scanQty,
                        seqNo = 1,
                        formCode = formCode
                    )
                    checkingHistoryRepo.save(domain)
                } else {
                    val newScanQty = data.scanQty?.plus(request.scanQty)
                    checkingHistoryRepo.update(newScanQty!!, scanDate, data.poNumber!!, 1)
                }
            } else {
                val data = checkingHistoryRepo.findLatestByScanDateAndPO(scanDate, request.poNumber)
                    ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.checkingHistory"), "scanDate = ${scanDate}, poNumber = ${request.poNumber}")
                val seqNo = data.seqNo?.plus(1)
                val domain = CheckingHistory(
                    poNumber = request.poNumber,
                    importQty = request.importQty,
                    scanQty = request.scanQty,
                    seqNo = seqNo,
                    formCode = formCode
                )
                checkingHistoryRepo.save(domain)
            }
        }
    }

    private fun getFormCode(): String {
        val datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val latestSuffix = checkingHistoryRepo.findLatestHistory()
            ?.formCode
            ?.substringAfter("-")
            ?.let { suffix ->
                (suffix.toIntOrNull()?.plus(1))
                    ?.toString()
                    ?.padStart(suffix.length, '0')
            }

        return "$datePrefix-${latestSuffix ?: "01"}"
    }
}