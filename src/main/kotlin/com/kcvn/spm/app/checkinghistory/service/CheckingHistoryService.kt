package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.PurchaseOrderBacklogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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

    fun save(requestList: List<CheckingHistoryRequest>) {
        val scanDate = LocalDate.now()

        requestList.forEach { request ->
            val order = orderBacklogRepo.findLotOfPoInvoice(request.poNumber, request.invoiceNumber)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${request.poNumber}-${request.invoiceNumber}"))
            if (order.approved == true) throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm ${request.poNumber}-${request.invoiceNumber}"))
            if (!request.reChecking) {
                val data = checkingTransRepo.findByLotAndSeq(request, 1)
                if (data == null) {
                    val domain = NewCheckingTransaction(
                        invoiceNumber = request.invoiceNumber,
                        poNumber = request.poNumber,
                        department = order.prodGroup,
                        storageLocation = order.storageLocation,
                        itemType = order.itemType,
                        scanQty = request.qty,
                        lotNo = request.lotNo,
                        checkTimes = 1,
                        receivingDate = request.receivingDate,
                        specifyInvoice = request.specifyInvoice,
                        isApprove = false,
                        isSyncedSap = false
                    )
                    checkingTransRepo.save(domain)
                } else {

                    val newScanQty = data.scanQty?.plus(request.qty)
                    checkingTransRepo.update(newScanQty!!, scanDate, data.poNumber!!, 1, data.formCode!!)
                }
            } else {
                val data = checkingTransRepo.findLatestSeqOfLot(request)
                    ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.checkingHistory"), "scanDate = ${scanDate}, poNumber = ${request.poNumber}")
                val seqNo = data.checkTimes?.plus(1)
                val domain = NewCheckingTransaction(
                    invoiceNumber = request.invoiceNumber,
                    poNumber = request.poNumber,
                    department = order.prodGroup,
                    storageLocation = order.storageLocation,
                    itemType = order.itemType,
                    scanQty = request.qty,
                    lotNo = request.lotNo,
                    checkTimes = seqNo,
                    receivingDate = request.receivingDate,
                    specifyInvoice = request.specifyInvoice,
                    isApprove = false,
                    isSyncedSap = false
                )
                checkingTransRepo.save(domain)
            }
        }
    }
}