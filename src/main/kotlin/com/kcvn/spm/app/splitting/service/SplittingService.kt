package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.payload.respone.SplittingResponse
import com.kcvn.spm.app.splitting.payload.request.TempBacklogWhSearchRequest
import com.kcvn.spm.app.splitting.payload.respone.TempBacklogWhResponse
import com.kcvn.spm.app.splitting.payload.respone.TempSplittingResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import org.springframework.data.domain.Pageable
import com.kcvn.spm.model.tables.pojos.TempNewBacklogWh
import com.kcvn.spm.model.tables.pojos.TempSplitting
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import com.kcvn.spm.repository.SplittingRepository
import com.kcvn.spm.repository.TempNewBacklogWhRepository
import com.kcvn.spm.repository.TempSplittingRepository
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
    private val tempSplittingRepo: TempSplittingRepository,
    private val tempNewBacklogWhRepo: TempNewBacklogWhRepository,
) {

    fun getTempPackageList(request: TempBacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<TempSplittingResponse> {
        val data = tempSplittingRepo.getPackageList(request, pageable)
        return BasePagingResponse(
            data.first,
            data.second
        )
    }

    fun getTempPackageDetail(tempSplittingId: String, pageable: Pageable): BasePagingResponse<TempBacklogWhResponse> {
        tempSplittingRepo.findById(tempSplittingId)
            ?: throw BusinessException("Không tìm thấy package chia hàng: $tempSplittingId")
        val data = tempNewBacklogWhRepo.findDetailByTempSplittingId(tempSplittingId, pageable)
        return BasePagingResponse(
            data.first,
            data.second
        )
    }

    fun deleteTempPackage(tempSplittingId: String) {
        //update co dieu kien (chua xoa, chua duyet) => tranh xoa trung / xoa package vua duoc duyet
        if (tempSplittingRepo.softDeletePending(tempSplittingId) == 0) {
            val existing = tempSplittingRepo.findById(tempSplittingId)
                ?: throw BusinessException("Không tìm thấy package chia hàng: $tempSplittingId")
            if (existing.isApproved == true)
                throw BusinessException("Package ${existing.packageCode} đã được duyệt, không thể xóa")
            throw BusinessException("Không thể xóa package ${existing.packageCode}")
        }
    }

    fun saveSplitting(request: List<SplittingRequest>): List<String> {
        //kiem tra package code xuat hien nhieu lan (tren toan bo request)
        val duplicatePackageSet = request.groupBy { it.packageCode }
            .filter { pkg -> pkg.value.map { Triple(it.receivingDate, it.binCode, it.poNumber)}.distinct().size > 1 }.keys
        if (duplicatePackageSet.isNotEmpty())
            throw BusinessException("Có nhiều ngày nhập/BIN/PO: ${duplicatePackageSet.joinToString(", ")}")

        val packageInvalidList = mutableListOf<Pair<String, String>>()
        //kiem tra package da ton tai truoc, bo ra khoi request de khong anh huong kiem tra so luong
        val existPackageSet = request.map { it.packageCode }.distinct()
            .filter { splittingRepo.findByPackage(it) != null || tempSplittingRepo.findPendingByPackage(it) != null }.toSet()
        existPackageSet.forEach { packageInvalidList.add(Pair(it, "Package $it đã tồn tại")) }
        val newPackageRequest = request.filter { it.packageCode !in existPackageSet }

        val receivingDateList = newPackageRequest.map { it.receivingDate }.distinct()

        //PO khong hop le theo tung ngay nhap
        val invalidPoSet = mutableSetOf<Pair<LocalDate, String>>()
        ///kiem tra so luong chia hang co khop voi so luong nhap khong
        receivingDateList.forEach{ date ->
            val checkedList = newCheckTransactionRepo.findByRecDate(date).filter { it.isApprove == true }.filter { it.storageLocation in listOf("L000","L001") }
            val receivedList = receivingTransactionRepo.findByRecDate(date)
            //so luong da chia nhung dang cho duyet (chua vao receiving_transactions) cung tinh la da nhap
            val pendingList = tempNewBacklogWhRepo.findPendingByRecDate(date)
            val requestOnThisDate = newPackageRequest.filter { it.receivingDate == date }
            val requestOnThisDateByPo = requestOnThisDate.groupBy { it.poNumber}
            //kiem tra theo PO
            requestOnThisDateByPo.forEach{ po ->
                val requestPoQty = po.value.sumOf { it.quantity }
                val receivedPoQty = receivedList.filter { it.poNumber == po.key }.sumOf { it.qty ?: BigDecimal.ZERO } +
                        pendingList.filter { it.poNumber == po.key }.sumOf { it.backlogQty ?: BigDecimal.ZERO }
                val checkedPoQty = checkedList.filter { it.poNumber == po.key }.sumOf { it.scanQty!! }
                if (checkedPoQty compareTo (requestPoQty + receivedPoQty) != 0) {
                    packageInvalidList.add(Pair(po.key, "PO ${po.key} không khớp số lượng đã nhập"))
                    invalidPoSet.add(date to po.key)
                }
                val requestOnThisDateByPoAndLot = po.value.groupBy { it.lotNo }
                //kiem tra theo Lot
                requestOnThisDateByPoAndLot.forEach{ lot ->
                    val requestLotQty = lot.value.sumOf { it.quantity }
                    val receivedLotQty = receivedList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.qty?: BigDecimal.ZERO } +
                            pendingList.filter { it.poNumber == po.key && it.lotNo == lot.key }.sumOf { it.backlogQty ?: BigDecimal.ZERO }
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
            TempSplitting(
                receivingDate =  it.receivingDate,
                packageCode = it.packageCode,
                locationCode = it.binCode,
            )
        }.distinct()

        //luu vao bang tam, cho duyet moi chuyen sang splitting / new_backlog_wh
        splitingData.forEach { split ->
            val tempSplittingId = tempSplittingRepo.save(split)
            //moi dong request = 1 dong tam (khong gop), khi approve map 1-1 sang saveRecTrans
            validQtyList.filter { it.packageCode == split.packageCode }
                .forEach { row ->
                    val checkingRecord = newCheckTransactionRepo.findByRecDatePoAndLot(
                        row.receivingDate,
                        row.poNumber,
                        row.lotNo
                    ).firstOrNull() //vi 1 lot, 1 PO co the thuoc nhieu invoice, nhung phai trung cac thong tin khac, nen lay dong dau tien
                    tempNewBacklogWhRepo.save(
                        TempNewBacklogWh(
                            receivingDate = row.receivingDate,
                            poNumber = row.poNumber,
                            packageCode = row.packageCode,
                            backlogQty = row.quantity,
                            boxQty = 1,
                            itemCode = checkingRecord?.itemCode,
                            itemName = checkingRecord?.itemName,
                            productionGroup = checkingRecord?.department,
                            lotNo = row.lotNo,
                            issueDate = row.issueDate?.toString(),
                            storageLocation = checkingRecord?.storageLocation,
                            itemType = checkingRecord?.itemType,
                            unit = checkingRecord?.unit,
                            unitPrice = checkingRecord?.unitPrice,
                            costCenter = checkingRecord?.costCenter,
                            tempSplittingId = tempSplittingId,
                        )
                    )
                }
        }
        return packageInvalidList.map { it.second}
    }
}