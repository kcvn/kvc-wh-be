package com.kcvn.spm.app.TransferLot.service

import com.kcvn.spm.app.TransferLot.payload.request.TransferLotRequest
import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.service.SplittingService
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class TransferLotService(
    private val newCheckTransactionRepo: NewCheckingTransactionRepository,
    private val receivingTransactionRepo: ReceivingTransactionsRepository,
    private val splittingService: SplittingService,
) {
    fun saveTransfer(request: List<TransferLotRequest>): List<String> {
        //kiem tra package code xuat hien nhieu lan (tren toan bo request)
        val duplicatePackageSet = request.groupBy { it.packageCode }
            .filter { pkg -> pkg.value.map { Triple(it.receivingDate, it.binCode, it.poNumber)}.distinct().size > 1 }.keys
        if (duplicatePackageSet.isNotEmpty())
            throw BusinessException("Có nhiều ngày nhập/BIN/PO: ${duplicatePackageSet.joinToString(", ")}")

        val packageInvalidList = mutableListOf<Pair<String, String>>()
        //kiem tra package da ton tai truoc (da duyet hoac dang cho duyet), bo ra khoi request de khong anh huong kiem tra so luong
        val existPackageSet = request.map { it.packageCode }.distinct()
            .filter { splittingService.isPackageExisted(it) }.toSet()
        existPackageSet.forEach { packageInvalidList.add(Pair(it, "Package $it đã tồn tại")) }
        //kiem tra lot (= packageCode) da duoc chia hang thuong (da duyet hoac dang cho duyet) => khong cho chuyen lot
        val splittedLotSet = request.filter { it.packageCode !in existPackageSet }
            .map { Triple(it.receivingDate, it.poNumber, it.packageCode) }.distinct()
            .filter { (date, po, lot) -> splittingService.isLotSplitted(date, po, lot) }
            .map { it.third }.toSet()
        splittedLotSet.forEach { packageInvalidList.add(Pair(it, "Lot $it đã được chia hàng, không thể chuyển lot")) }
        val newPackageRequest = request.filter { it.packageCode !in existPackageSet && it.packageCode !in splittedLotSet }

        val receivingDateList = newPackageRequest.map { it.receivingDate }.distinct()

        //package khong khop so luong
        val invalidPackageSet = mutableSetOf<String>()
        ///kiem tra so luong theo package: da check (lot no = package code) = da nhap + lan nay
        //(package dang cho duyet da bi chan o tren nen khong can cong them so cho duyet)
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

        //luu vao bang tam giong chia hang, cho duyet (approve dung chung API /api/splitting/temp/approve)
        splittingService.saveTempPackages(
            validQtyList.map {
                SplittingRequest(
                    receivingDate = it.receivingDate,
                    binCode = it.binCode,
                    packageCode = it.packageCode,
                    poNumber = it.poNumber,
                    lotNo = it.lotNo,
                    quantity = it.quantity,
                    issueDate = it.issueDate,
                )
            },
            isTransferLot = true
        )
        return packageInvalidList.map { it.second}
    }
}
