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
        val receivingDateList = request.map { it.receivingDate }.distinct()

        val packageInvalidList = mutableListOf<Pair<String, String>>()
        ///kiem tra so luong chia hang co khop voi so luong nhap khong
        receivingDateList.forEach{ date ->
            val checkedList = newCheckTransactionRepo.findByRecDate(date).filter { it.isApprove == true }
            val receivedList = receivingTransactionRepo.findByRecDate(date)
            val requestOnThisDate = request.filter { it.receivingDate == date }
            val requestOnThisDateByPo = requestOnThisDate.groupBy { it.poNumber}
            //kiem tra theo PO
            requestOnThisDateByPo.forEach{ po ->
                val requestPoQty = po.value.sumOf { it.quantity }
                val receivedPoQty = receivedList.filter { it.poNumber == po.key }.sumOf { it.qty ?: BigDecimal.ZERO }
                val checkedPoQty = checkedList.filter { it.poNumber == po.key }.sumOf { it.scanQty!! }
                if (checkedPoQty != requestPoQty + receivedPoQty) packageInvalidList.add(Pair(po.key, "PO ${po.key} không khớp số lượng đã nhập"))
                val requestOnThisDateByPoAndLot = po.value.groupBy { it.lotNo }
                //kiem tra theo Lot
                requestOnThisDateByPoAndLot.forEach{ lot ->
                    val requestLotQty = lot.value.sumOf { it.quantity }
                    val receivedLotQty = receivedList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.qty?: BigDecimal.ZERO }
                    val checkedLotQty = checkedList.filter { it.poNumber == po.key && it.lotNo == lot.key}.sumOf { it.scanQty!! }
                    if (requestLotQty != checkedLotQty + receivedLotQty) packageInvalidList.add(Pair(po.key, "PO ${po.key} - ${lot.key} không khớp số lượng đã nhập"))
                }
            }
        }
        val invalidQtySet = packageInvalidList.map { it.first }.toSet()
        val validQtyList = request.filter { it.poNumber !in invalidQtySet }
        val splitingData = validQtyList.map {
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

        splitingData.filter { it.packageCode !in duplicatePackageSet }.forEach { split ->
            val exist = splittingRepo.findByPackage(split.packageCode!!)
            if (exist != null) packageInvalidList.add(Pair(split.packageCode!!, "Đã tồn tại"))
            else
            {
                splittingRepo.save(split)
                val lotData = validQtyList.filter { it.packageCode == split.packageCode }
                val receivingData = lotData.map { RecTransRequest(
                    locationCode = it.binCode,
                    packageCode = it.packageCode,
                    poNumber = it.poNumber,
                    qty = it.quantity,
                    lotNo = it.lotNo,
                    issueDate = it.issueDate.toString()
                ) }
                receivingService.saveRecTrans(receivingData,split.receivingDate)
            }
        }
        return packageInvalidList.map { it.second}
    }
}