package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.PurchaseOrderBacklogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class CheckingHistoryService(
    private val checkingTransRepo: NewCheckingTransactionRepository,
    private val orderBacklogRepo: PurchaseOrderBacklogRepository,
) {
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable): BasePagingResponse<CheckingHistoryResponse> {
        val checkingHistoryData = checkingTransRepo.searchCheckingHistory(request, pageable)
        val data = checkingHistoryData.first
        return BasePagingResponse(
            data,
            checkingHistoryData.second
        )
    }
//
//    fun getListDetail(lotNo: String, pageable: Pageable): BasePagingResponse<CheckingHistoryDetailResponse> {
//        val checkingHistoryDataDetail = checkingHistoryRepo.getListDetail(lotNo, pageable)
//        val data = checkingHistoryDataDetail.first
//        return BasePagingResponse(
//            data,
//            checkingHistoryDataDetail.second
//        )
//    }

    fun saveCheckingTransaction(requestList: List<CheckingHistoryRequest>) {
        val specifyInvoice = requestList.filter { it.specifyInvoice }.groupBy { it.poNumber to it.invoiceNumber }
        val notSpecifyInvoice = requestList.filter { !it.specifyInvoice }.groupBy { it.poNumber }

        specifyInvoice.forEach { request ->
            val order = orderBacklogRepo.findLotOfPoInvoice(request.key.first, request.key.second)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${request.key.first}-${request.key.second}"))
            if (order.approved == true)
                throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm ${request.key.first}" +
                        "-${request.key.second}"))
            val latest = checkingTransRepo.findLatestSeqOfOrder(request.key.first, request.key.second)
            request.value.forEach {
                if (!it.reChecking) {
                    val existing = checkingTransRepo.findByLotAndSeq(it, 1)
                    if (existing == null) {
                        val domain = NewCheckingTransaction(
                            invoiceNumber = it.invoiceNumber,
                            poNumber = it.poNumber,
                            department = order.prodGroup,
                            storageLocation = order.storageLocation,
                            itemType = order.itemType,
                            scanQty = it.qty,
                            lotNo = it.lotNo,
                            checkTimes = 1,
                            receivingDate = it.receivingDate,
                            specifyInvoice = it.specifyInvoice,
                            isApprove = false,
                            isSyncedSap = false
                        )
                        checkingTransRepo.save(domain)
                    } else {
                        existing.scanQty = (existing.scanQty ?: BigDecimal.ZERO) + it.qty
                        checkingTransRepo.update(existing)
                    }
                } else {
                    val seqNo = latest?.checkTimes?.plus(1)
                        ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.checkingHistory"), "${request.key.first}-${request.key.second}")
                    val domain = NewCheckingTransaction(
                        invoiceNumber = it.invoiceNumber,
                        poNumber = it.poNumber,
                        department = order.prodGroup,
                        storageLocation = order.storageLocation,
                        itemType = order.itemType,
                        scanQty = it.qty,
                        lotNo = it.lotNo,
                        checkTimes = seqNo,
                        receivingDate = it.receivingDate,
                        specifyInvoice = it.specifyInvoice,
                        isApprove = false,
                        isSyncedSap = false
                    )
                    checkingTransRepo.save(domain)
                }
            }
        }


    }
}