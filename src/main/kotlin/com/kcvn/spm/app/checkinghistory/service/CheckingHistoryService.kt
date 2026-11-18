package com.kcvn.spm.app.checkinghistory.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryApproveRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryByLotResponse
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryDetailResponse
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistorySummaryResponse
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.plus

@Service
@Transactional
class CheckingHistoryService(
    private val checkingTransRepo: NewCheckingTransactionRepository,
    private val orderBacklogRepo: PurchaseOrderBacklogRepository,
) {
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable, isExport: Boolean): BasePagingResponse<CheckingHistoryResponse> {
        val checkingHistoryData = checkingTransRepo.searchCheckingHistory(request, pageable, isExport)
        val data = checkingHistoryData.first
        return BasePagingResponse(
            data,
            checkingHistoryData.second
        )
    }

    fun getSummary(request: CheckingHistorySearchRequest): CheckingHistorySummaryResponse {
        return checkingTransRepo.summaryCheckingHistory(request)
    }

    fun searchCheckingHistoryDetail(poNumber: String, invoiceNumber: String, seqNo: Int, pageable: Pageable): BasePagingResponse<CheckingHistoryDetailResponse> {
        val checkingHistoryDataDetail = checkingTransRepo.searchCheckingHistoryDetail(poNumber, invoiceNumber, seqNo, pageable)
        val data = checkingHistoryDataDetail.first
        return BasePagingResponse(
            data,
            checkingHistoryDataDetail.second
        )
    }

    fun getListApproved(recDate: LocalDate, storageLocationList: List<String>): List<CheckingHistoryByLotResponse> {
        val checkingHistoryData = checkingTransRepo.findByRecDate(recDate).filter { it.isApprove == true }.filter { it.storageLocation in storageLocationList }
        if (checkingHistoryData.isEmpty()) throw BusinessException("Không có dữ liệu check hàng ngày $recDate")
        val checkingHistoryByLotData = checkingHistoryData.map { CheckingHistoryByLotResponse(
            checkingHistoryId = it.id,
            receivingDate = it.receivingDate,
            poNumber = it.poNumber,
            invoiceNumber = it.invoiceNumber,
            department = it.department,
            storageLocation = it.storageLocation,
            lotNo = it.lotNo,
            itemType = it.itemType,
            seqNo = it.checkTimes,
            scanQty = it.scanQty,
            status = it.isApprove,
            scannedDate = it.createdDate?.toLocalDate(),
            scannedBy = it.createdBy,
        )}
        return checkingHistoryByLotData
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

        // seqNo check lại dùng chung cho mọi invoice của PO = lần check lớn nhất của PO + 1.
        // Tính 1 lần trước khi save, tránh seqNo tăng tiếp sau khi invoice trước của cùng PO đã save.
        val reCheckSeqNoByPo = requestList.filter { it.reChecking }
            .map { it.poNumber }
            .distinct()
            .associateWith { (checkingTransRepo.findLatestSeqOfPo(it) ?: 0) + 1 }

        specifyInvoice.forEach { request ->
            if (request.key.second == "File lam tem goi") return@forEach
            val order = orderBacklogRepo.findOrderByPoInvoice(request.key.first, request.key.second)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order ${request.key.first}-${request.key.second}"))
            if (order.approved == true)
                throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm ${request.key.first}" +
                        "-${request.key.second}"))
            request.value.forEach {
                saveOrUpdate(it, order, if (it.reChecking) reCheckSeqNoByPo.getValue(it.poNumber) else 1)
            }
        }

        notSpecifyInvoiceNotReCheck.forEach { (poNumber, lots) -> allocateByInvoice(poNumber, lots, 1) }
        notSpecifyInvoiceReCheck.forEach { (poNumber, lots) -> allocateByInvoice(poNumber, lots, reCheckSeqNoByPo.getValue(poNumber)) }
    }

    private fun allocateByInvoice(poNumber: String, lots: List<CheckingHistoryRequest>, seqNo: Int) {
        // Chỉ lặp qua các invoice có trong request, sắp theo invoice tăng dần
        val orders = lots.map { it.invoiceNumber }.distinct().sorted().map { invoiceNumber ->
            val order = orderBacklogRepo.findOrderByPoInvoice(poNumber, invoiceNumber)
                ?: throw BusinessException(CommonUtils.getMessage("Không tìm thấy order $poNumber-$invoiceNumber"))
            if (order.approved == true)
                throw BusinessException(CommonUtils.getMessage("Order đã được duyệt, không thể scan thêm $poNumber-$invoiceNumber"))
            order
        }

        // lots giữ nguyên thứ tự như trong request, chỉ invoice được sắp tăng dần
        lots.forEach { lot ->
            if (lot.qty == BigDecimal.ZERO) return@forEach //bỏ qua các record rác, các record này chỉ để lấy danh sách invoice đã chọn
            var remaining = lot.qty
            for ((i, order) in orders.withIndex()) {
                if (remaining <= BigDecimal.ZERO) break
                val invoiceNumber = order.invoiceNumber!!
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
                itemName = order.itemName,
                itemCode = order.itemCode,
                unit = order.unit,
                unitPrice = order.unitPrice,
                costCenter = order.costCenter,
                isApprove = false,
                isSyncedSap = false,
            )
            checkingTransRepo.save(domain)
        } else {
            existing.scanQty = (existing.scanQty ?: BigDecimal.ZERO) + request.qty
            checkingTransRepo.update(existing)
        }
    }

    // file txt (tab-separated, không header): receivingDate, invoice, po, 1, storageLocation, lotNo, receiveQty, unit
    // chỉ lấy các lần check (PO-invoice-seqNo) được chọn, đã duyệt và chưa sync SAP
    // lấy danh sách xong thì đánh dấu đã sync SAP luôn (cùng transaction)
    fun exportApprovedNotSyncedSapTxt(requestList: List<CheckingHistoryApproveRequest>): ByteArray {
        if (requestList.isEmpty()) throw BusinessException(CommonUtils.getMessage("Chưa chọn dữ liệu để xuất SAP"))
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val lots = checkingTransRepo.getApprovedNotSyncedSapLots(requestList)
        if (lots.isEmpty()) throw BusinessException(CommonUtils.getMessage("Không có dữ liệu đã duyệt và chưa sync SAP trong các dòng đã chọn"))
        checkingTransRepo.markSyncedSap(lots.mapNotNull { it.id })
        val content = lots.joinToString(separator = "\r\n") {
            listOf(
                it.receivingDate?.format(dateFormatter).orEmpty(),
                it.invoiceNumber.orEmpty(),
                it.poNumber.orEmpty(),
                "00001",
                it.storageLocation.orEmpty(),
                it.lotNo.orEmpty(),
                (it.scanQty ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString(),
                it.unit.orEmpty(),
            ).joinToString("\t")
        }
        return content.toByteArray(Charsets.UTF_8)
    }

    fun approveCheckingTransaction(requestList: List<CheckingHistoryApproveRequest>) {
        requestList.map { it.poNumber to it.invoiceNumber }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (poNumber, invoiceNumber) -> orderBacklogRepo.lockOrderByPoInvoice(poNumber, invoiceNumber) }

        validateSelectAllPoInInvoice(requestList)
        validateSameSeqNoOfPo(requestList)
        requestList.forEach { processEachPOInvoice(it) }
    }

    // 1 PO thuộc nhiều invoice thì các invoice chưa duyệt của PO đó phải được check chung 1 lần và duyệt chung 1 lần:
    // phải chọn đủ tất cả invoice chưa duyệt của PO, và seqNo được chọn phải bằng nhau
    fun validateSameSeqNoOfPo(requestList: List<CheckingHistoryApproveRequest>) {
        requestList.groupBy { it.poNumber }.forEach { (poNumber, requests) ->
            val notApprovedInvoices = orderBacklogRepo.findOrderByPo(poNumber)
                .filter { it.approved != true }
                .mapNotNull { it.invoiceNumber }
                .toSet()
            val missingInvoices = notApprovedInvoices - requests.map { it.invoiceNumber }.toSet()
            if (missingInvoices.isNotEmpty())
                throw BusinessException(CommonUtils.getMessage("PO $poNumber thuộc nhiều invoice, phải duyệt cùng lúc tất cả invoice chưa duyệt, thiếu: " +
                        missingInvoices.sorted().joinToString()))

            if (requests.map { it.seqNo }.distinct().size > 1)
                throw BusinessException(CommonUtils.getMessage("PO $poNumber thuộc nhiều invoice, phải chọn cùng 1 lần check: " +
                        requests.joinToString { "${it.invoiceNumber} (lần ${it.seqNo})" }))
        }
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