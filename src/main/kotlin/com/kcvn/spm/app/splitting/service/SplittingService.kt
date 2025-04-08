package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.util.CommonUtils
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
    fun saveSplitting(request: List<SplittingRequest>) {
        request.forEach {
            val data = Splitting(
                null,
                it.receivingDate,
                it.packageCode,
                it.locationCode
            )
            // save splitting
            splittingRepo.save(data)
            // save receiving transaction, backlogWh, backlogWhHistory
            val checkingList = checkingRepo.getByPackageCode(it.packageCode!!)
            if (checkingList.isEmpty()) {
                throw BusinessExceptionDetail(CommonUtils.getMessage("Mã gói không tồn tại"), "packageCode = ${it.packageCode}")
            }
            val recTransRequestList = mutableListOf<RecTransRequest>()
            checkingList.forEach { ck ->
                val recTrans = RecTransRequest(
                    locationCode = it.locationCode,
                    packageCode = it.packageCode,
                    poNumber = ck.poNumber,
                    qty = ck.qty
                )
                recTransRequestList.add(recTrans)
            }
            receivingService.saveRecTrans(recTransRequestList, it.receivingDate)
        }
    }
}