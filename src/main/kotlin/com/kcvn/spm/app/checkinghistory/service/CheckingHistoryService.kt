package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryApproveRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryDetailResponse
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.PurchaseOrderBacklogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.plus

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

    fun searchCheckingHistoryDetail(poNumber: String, invoiceNumber: String, seqNo: Int, pageable: Pageable): BasePagingResponse<CheckingHistoryDetailResponse> {
        val checkingHistoryDataDetail = checkingTransRepo.searchCheckingHistoryDetail(poNumber, invoiceNumber, seqNo, pageable)
        val data = checkingHistoryDataDetail.first
        return BasePagingResponse(
            data,
            checkingHistoryDataDetail.second
        )
    }

    fun saveCheckingTransaction(requestList: List<CheckingHistoryRequest>) {
        val specifyInvoice = requestList.filter { it.specifyInvoice }.groupBy { it.poNumber to it.invoiceNumber }
        val notSpecifyInvoiceNotReCheck = requestList.filter { !it.specifyInvoice && !it.reChecking }.groupBy { it.poNumber }
        val notSpecifyInvoiceReCheck = requestList.filter { !it.specifyInvoice && it.reChecking }.groupBy { it.poNumber }

        // Khóa tất cả order liên quan trước khi đọc scanQty / seqNo, theo thứ tự PO-invoice cố định để tránh deadlock.
        // Request khác cùng order sẽ phải chờ transaction này commit rồi mới đọc được dữ liệu mới.
        requestList.map { it.poNumber to it.invoiceNumber }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (poNumber, invoiceNumber) -> orderBacklogRepo.lockOrderByPoInvoice(poNumber, invoiceNumber) }

        specifyInvoice.forEach { request ->
            if (request.key.second == "File lam tem goi") return@forEach
            val order = orderBacklogRepo.findOrderByPoInvoice(request.key.first, request.key.second)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${request.key.first}-${request.key.second}"))
            if (order.approved == true)
                throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm ${request.key.first}" +
                        "-${request.key.second}"))
            val latest = checkingTransRepo.findLatestSeqOfOrder(request.key.first, request.key.second)
            val seqNo = (latest?.checkTimes?: 0).plus(1)
            request.value.forEach {
                if (!it.reChecking) {
                    saveOrUpdate(it, order, 1)
                } else {
                    saveOrUpdate(it, order, seqNo)
                }
            }
        }

        notSpecifyInvoiceNotReCheck.forEach { (poNumber, lots) -> allocateByInvoice(poNumber, lots, false) }
        notSpecifyInvoiceReCheck.forEach { (poNumber, lots) -> allocateByInvoice(poNumber, lots, true) }
    }

    private fun allocateByInvoice(poNumber: String, lots: List<CheckingHistoryRequest>, reChecking: Boolean) {
        // Chỉ lặp qua các invoice có trong request, sắp theo invoice tăng dần
        val orders = lots.map { it.invoiceNumber }.distinct().sorted().map { invoiceNumber ->
            val order = orderBacklogRepo.findOrderByPoInvoice(poNumber, invoiceNumber)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order $poNumber-$invoiceNumber"))
            if (order.approved == true)
                throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm $poNumber-$invoiceNumber"))
            order
        }

        // Tính seqNo 1 lần cho mỗi invoice, tránh tăng seqNo sau khi lot trước đã save
        val seqNoByInvoice = orders.associate { order ->
            val invoiceNumber = order.invoiceNumber!!
            val seqNo = if (reChecking) {
                (checkingTransRepo.findLatestSeqOfOrder(poNumber, invoiceNumber)?.checkTimes ?: 0) + 1
            } else 1
            invoiceNumber to seqNo
        }

        // lots giữ nguyên thứ tự như trong request, chỉ invoice được sắp tăng dần
        lots.forEach { lot ->
            if (lot.qty == BigDecimal.ZERO) return@forEach //bỏ qua các record rác, các record này chỉ để lấy danh sách invoice đã chọn
            var remaining = lot.qty
            for ((i, order) in orders.withIndex()) {
                if (remaining <= BigDecimal.ZERO) break
                val invoiceNumber = order.invoiceNumber!!
                val seqNo = seqNoByInvoice.getValue(invoiceNumber)
                val isLast = i == orders.lastIndex

                val scannedQty = checkingTransRepo.findByPoInvoiceAndSeq(poNumber, invoiceNumber, seqNo)
                    ?.sumOf { it.scanQty ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
                val available = (order.orderQty ?: BigDecimal.ZERO) - scannedQty
                // Invoice cuối nhận hết phần dư
                val qtyForThis = if (isLast) remaining else minOf(remaining, available)
                if (qtyForThis <= BigDecimal.ZERO) continue

                saveOrUpdate(lot.copy(invoiceNumber = invoiceNumber, qty = qtyForThis), order, seqNo)
                remaining -= qtyForThis
            }
        }
    }

    fun saveOrUpdate(request: CheckingHistoryRequest, order: PurchaseOrderBacklog, seqNo: Int) {
        val existing = checkingTransRepo.findByPoInvoiceLotAndSeq(request.invoiceNumber, request.poNumber, request.lotNo!!, seqNo)
        if (existing == null) {
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
        } else {
            existing.scanQty = (existing.scanQty ?: BigDecimal.ZERO) + request.qty
            checkingTransRepo.update(existing)
        }
    }

    fun approveCheckingTransaction(requestList: List<CheckingHistoryApproveRequest>) {
        requestList.map { it.poNumber to it.invoiceNumber }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (poNumber, invoiceNumber) -> orderBacklogRepo.lockOrderByPoInvoice(poNumber, invoiceNumber) }

        validateSelectAllPoInInvoice(requestList)
        requestList.forEach { processEachPOInvoice(it) }
    }

    fun validateSelectAllPoInInvoice(requestList: List<CheckingHistoryApproveRequest>) {
        requestList.groupBy { it.invoiceNumber }.forEach { (invoiceNumber, requests) ->
            val duplicatedPO = requests.groupBy { it.poNumber }.filterValues { it.size > 1 }.keys
            if (duplicatedPO.isNotEmpty())
                throw BusinessException(CommonUtils.getMessage("Invoice $invoiceNumber có PO bị chọn nhiều lần: ${duplicatedPO.joinToString()}"))

            val allOrderPO = orderBacklogRepo.findOrderByInvoice(invoiceNumber)
                ?.mapNotNull { it.poNumber }?.toSet().orEmpty()
            val allRequestPO = requests.map { it.poNumber }.toSet()

            val missingPO = allOrderPO - allRequestPO
            if (missingPO.isNotEmpty())
                throw BusinessException(CommonUtils.getMessage("Invoice $invoiceNumber chưa chọn đủ PO, thiếu: ${missingPO.joinToString()}"))

            val unknownPO = allRequestPO - allOrderPO
            if (unknownPO.isNotEmpty())
                throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${unknownPO.joinToString { "$it-$invoiceNumber" }}"))
        }
    }

    fun processEachPOInvoice(request: CheckingHistoryApproveRequest){
        //validate order is enough
        val order = orderBacklogRepo.findOrderByPoInvoice(request.poNumber, request.invoiceNumber)
            ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${request.poNumber}-${request.invoiceNumber}"))
        if (order.approved == true)
            throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể duyệt thêm ${request.poNumber}-${request.invoiceNumber}"))
        val orderQty = order.orderQty

        val scanned = checkingTransRepo.findByPoInvoiceAndSeq(request.poNumber, request.invoiceNumber, request.seqNo)
        val scannedQty = scanned?.sumOf { it.scanQty?: BigDecimal.ZERO } ?: BigDecimal.ZERO
        if (scannedQty.compareTo(orderQty ?: BigDecimal.ZERO) != 0)
            throw BusinessException(CommonUtils.getMessage("Chưa check đủ số lượng order ${request.poNumber}-${request.invoiceNumber}, orderQty=${order.orderQty}, scannedQty=${scannedQty} "))
        order.approved = true
        orderBacklogRepo.update(order)
        scanned?.forEach { it.isApprove = true
        checkingTransRepo.update(it)
        }
    }


}