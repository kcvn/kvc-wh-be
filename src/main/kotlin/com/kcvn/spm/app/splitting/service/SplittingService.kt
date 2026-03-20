package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.repository.CheckingRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SplittingService(
    private val splittingRepo: SplittingRepository,
    private val checkingRepo: CheckingRepository,
    private val receivingService: ReceivingTransactionsService
) {
    fun saveSplitting(request: List<SplittingRequest>): List<String> {
        val packageInvalidList = mutableListOf<String>()
        request.forEach {
            val checkingList = checkingRepo.getByPackageCode(it.packageCode!!)
            val splittingData = splittingRepo.findByPackage(it.packageCode!!)
            if (checkingList.isEmpty()) {
                packageInvalidList.add(it.packageCode!!)
                return@forEach
            }
            if (splittingData != null) {
                return@forEach
            }
            val data = Splitting(
                null,
                it.receivingDate,
                it.packageCode,
                it.locationCode
            )
            // save splitting
            splittingRepo.save(data)
            // save receiving transaction, backlogWh, backlogWhHistory
            val recTransRequestList = mutableListOf<RecTransRequest>()
            checkingList.forEach { ck ->
                val recTrans = RecTransRequest(
                    locationCode = it.locationCode,
                    packageCode = it.packageCode,
                    poNumber = ck.poNumber,
                    qty = ck.qty,
                    lotNo = ck.lotNo,
                    issueDate = ck.issueDate
                )
                recTransRequestList.add(recTrans)
            }
            receivingService.saveRecTrans(recTransRequestList, it.receivingDate)
        }
        return packageInvalidList
    }
}