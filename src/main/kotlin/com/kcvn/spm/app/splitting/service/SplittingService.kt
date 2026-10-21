package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.payload.respone.SplittingResponse
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.repository.CheckingRepository
import com.kcvn.spm.repository.NewCheckingTransactionRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SplittingService(
    private val splittingRepo: SplittingRepository,
    private val newCheckTransactionRepo: NewCheckingTransactionRepository,
    private val receivingService: ReceivingTransactionsService,
) {
    fun saveSplitting(request: List<SplittingRequest>): List<SplittingResponse> {
        val receivingDateList = request.map { it.receivingDate }.distinct()

        val packageInvalidList = mutableListOf<Pair<String, String>>()
        ///kiem tra so luong chia hang co khop voi so luong nhap khong
        receivingDateList.forEach{ date ->
            val receivingList = newCheckTransactionRepo.findByRecDate(date).filter { it.isApprove == true }
            val requestOnThisDate = request.filter { it.receivingDate == date }
            val requestOnThisDateByPo = requestOnThisDate.groupBy { it.poNumber}
            //kiem tra theo PO
            requestOnThisDateByPo.forEach{ po ->
                val requestPoQty = po.value.sumOf { it.quantity }
                val receivedPoQty = receivingList.filter { it.poNumber == po.key }.sumOf { it.scanQty!! }
                if (receivedPoQty != requestPoQty) packageInvalidList.add(Pair(po.key, "PO không khớp số lượng đã nhập"))
                val requestOnThisDateByPoAndLot = po.value.groupBy { it.lotNo }
                //kiem tra theo Lot
                requestOnThisDateByPoAndLot.forEach{ lot ->
                    val requestLotQty = lot.value.sumOf { it.quantity }
                    val receivedLotQty = receivingList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.scanQty!! }
                    if (requestLotQty != receivedLotQty) packageInvalidList.add(Pair(po.key, "Lot ${po.key} - ${lot.key} không khớp số lượng đã nhập"))
                }
            }
        }
        val invalidPoSet = packageInvalidList.map { it.first }.toSet()
        val validList = request.filter { it.poNumber !in invalidPoSet }
        val splitingData = validList.map {
            Splitting(
                receivingDate =  it.receivingDate,
                packageCode = it.packageCode,
                locationCode = it.binCode
            )
        }.distinct()
        //kiem tra package code xuat hien nhieu lan
        val duplicatePackageSet = splitingData.groupBy { it.packageCode }.filter { it.value.size > 1 }.keys
        if (duplicatePackageSet.isNotEmpty())
            throw BusinessException("Có nhiều ngày nhập/BIN: ${duplicatePackageSet.joinToString(", ")}")

        splitingData.filter { it.packageCode !in duplicatePackageSet }.forEach {
            splittingRepo.save(it)
        }


        validList.forEach {
            // save receiving transaction, backlogWh, backlogWhHistory
        }
        return packageInvalidList.map { SplittingResponse(
            it.first,
            it.second
        ) }
    }
}