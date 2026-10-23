package com.kcvn.spm.app.TransferLot.service

import com.kcvn.spm.app.TransferLot.payload.request.TransferLotRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class TransferLotService(
    private val splittingRepo: SplittingRepository,
    private val newCheckTransactionRepo: NewCheckingTransactionRepository,
    private val receivingTransactionRepo: ReceivingTransactionsRepository,
    private val backlogWhRepo: BacklogWhRepository,
    private val receivingService: ReceivingTransactionsService,
) {
    fun saveTransfer(request: List<TransferLotRequest>): List<String> {
        //kiem tra package code xuat hien nhieu lan (tren toan bo request)
        val duplicatePackageSet = request.groupBy { it.packageCode }
            .filter { pkg -> pkg.value.map { Triple(it.receivingDate, it.binCode, it.poNumber)}.distinct().size > 1 }.keys
        if (duplicatePackageSet.isNotEmpty())
            throw BusinessException("Có nhiều ngày nhập/BIN/PO: ${duplicatePackageSet.joinToString(", ")}")

        val packageInvalidList = mutableListOf<Pair<String, String>>()
        //kiem tra package da ton tai truoc, bo ra khoi request de khong anh huong kiem tra so luong
        val existPackageSet = request.map { it.packageCode }.distinct()
            .filter { splittingRepo.findByPackage(it) != null }.toSet()
        existPackageSet.forEach { packageInvalidList.add(Pair(it, "Package $it đã tồn tại")) }
        val newPackageRequest = request.filter { it.packageCode !in existPackageSet }

        val receivingDateList = newPackageRequest.map { it.receivingDate }.distinct()

        //package khong khop so luong
        val invalidPackageSet = mutableSetOf<String>()
        ///kiem tra so luong theo package: da check (lot no = package code) = da nhap + lan nay
        receivingDateList.forEach{ date ->
            val checkedList = newCheckTransactionRepo.findByRecDate(date).filter { it.isApprove == true }
            val receivedList = receivingTransactionRepo.findByRecDate(date)
            val requestOnThisDateByPackage = newPackageRequest.filter { it.receivingDate == date }.groupBy { it.packageCode }
            requestOnThisDateByPackage.forEach{ pkg ->
                val requestPackageQty = pkg.value.sumOf { it.quantity }
                val receivedPackageQty = receivedList.filter { it.destPackageCode == pkg.key }.sumOf { it.qty ?: BigDecimal.ZERO }
                val checkedPackageQty = checkedList.filter { it.lotNo == pkg.key }.sumOf { it.scanQty!! }
                if (checkedPackageQty compareTo (requestPackageQty + receivedPackageQty) != 0) {
                    packageInvalidList.add(Pair(pkg.key, "Package ${pkg.key} không khớp số lượng đã nhập"))
                    invalidPackageSet.add(pkg.key)
                }
            }
        }
        val validQtyList = newPackageRequest.filter { it.packageCode !in invalidPackageSet }
        val splitingData = validQtyList.map {
            Splitting(
                receivingDate =  it.receivingDate,
                packageCode = it.packageCode,
                locationCode = it.binCode
            )
        }.distinct()

        splitingData.forEach { split ->
            splittingRepo.save(split)
            val lotData = validQtyList.filter { it.packageCode == split.packageCode }
            val receivingData = lotData.map { RecTransRequest(
                locationCode = it.binCode,
                packageCode = it.packageCode,
                poNumber = it.poNumber,
                qty = it.quantity,
                lotNo = it.lotNo,
                issueDate = it.issueDate?.toString()
            ) }
            receivingService.saveRecTrans(receivingData, split.receivingDate, isTransferLot = true)
        }
        return packageInvalidList.map { it.second}
    }
}