package com.kcvn.spm.app.splitting.service

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.CheckingRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SplittingService(
    private val splittingRepo: SplittingRepository,
    private val backlogWhRepo: BacklogWhRepository,
    private val checkingRepo: CheckingRepository
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
            // save backlogWh
            val checkingList = checkingRepo.getByPackageCode(it.packageCode!!)
            if (checkingList.isEmpty()) {
                throw BusinessException(CommonUtils.getMessage("Mã gói không tồn tại"))
            }
            val totalQty = checkingList.sumOf { checking -> checking.qty ?: 0 }
            val backlogWhData = BacklogWh(
                null,
                it.locationCode,
                checkingList[0].poNumber,
                checkingList[0].packageCode,
                totalQty,
                checkingList.size,
                it.receivingDate
            )
            backlogWhRepo.save(backlogWhData)
        }
    }
}