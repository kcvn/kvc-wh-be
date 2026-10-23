package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.payload.respone.SplittingResponse
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
import java.time.LocalDate

@Service
@Transactional
class SplittingService(
    private val splittingRepo: SplittingRepository,
    private val newCheckTransactionRepo: NewCheckingTransactionRepository,
    private val receivingTransactionRepo: ReceivingTransactionsRepository,
    private val backlogWhRepo: BacklogWhRepository,
    private val receivingService: ReceivingTransactionsService,
) {
    fun saveSplitting(request: List<SplittingRequest>): List<String> {
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

        //PO khong hop le theo tung ngay nhap
        val invalidPoSet = mutableSetOf<Pair<LocalDate, String>>()
        ///kiem tra so luong chia hang co khop voi so luong nhap khong
        receivingDateList.forEach{ date ->
            val checkedList = newCheckTransactionRepo.findByRecDate(date).filter { it.isApprove == true }.filter { it.storageLocation in listOf("L000","L001") }
            val receivedList = receivingTransactionRepo.findByRecDate(date)
            val requestOnThisDate = newPackageRequest.filter { it.receivingDate == date }
            val requestOnThisDateByPo = requestOnThisDate.groupBy { it.poNumber}
            //kiem tra theo PO
            requestOnThisDateByPo.forEach{ po ->
                val requestPoQty = po.value.sumOf { it.quantity }
                val receivedPoQty = receivedList.filter { it.poNumber == po.key }.sumOf { it.qty ?: BigDecimal.ZERO }
                val checkedPoQty = checkedList.filter { it.poNumber == po.key }.sumOf { it.scanQty!! }
                if (checkedPoQty compareTo (requestPoQty + receivedPoQty) != 0) {
                    packageInvalidList.add(Pair(po.key, "PO ${po.key} không khớp số lượng đã nhập"))
                    invalidPoSet.add(date to po.key)
                }
                val requestOnThisDateByPoAndLot = po.value.groupBy { it.lotNo }
                //kiem tra theo Lot
                requestOnThisDateByPoAndLot.forEach{ lot ->
                    val requestLotQty = lot.value.sumOf { it.quantity }
                    val receivedLotQty = receivedList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.qty?: BigDecimal.ZERO }
                    val checkedLotQty = checkedList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.scanQty!! }
                    if (checkedLotQty compareTo ( requestLotQty + receivedLotQty) != 0) {
                        packageInvalidList.add(Pair(po.key, "PO ${po.key} - ${lot.key} không khớp số lượng đã nhập"))
                        invalidPoSet.add(date to po.key)
                    }
                }
            }
        }
        //package co chua PO khong hop le thi bo ca package, khong luu thieu
        val invalidPackageSet = newPackageRequest.filter { (it.receivingDate to it.poNumber) in invalidPoSet }
            .map { it.packageCode }.toSet()
        invalidPackageSet.forEach { packageInvalidList.add(Pair(it, "Package $it có PO không khớp số lượng đã nhập")) }
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
            receivingService.saveRecTrans(receivingData,split.receivingDate)
        }
        return packageInvalidList.map { it.second}
    }
}