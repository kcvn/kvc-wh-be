package com.kcvn.spm.app.checking.service

import com.kcvn.spm.app.checking.payload.request.CheckingRequest
import com.kcvn.spm.app.checking.payload.request.CheckingRequestWithSeq
import com.kcvn.spm.model.tables.pojos.Checking
import com.kcvn.spm.repository.CheckingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CheckingService(
    private val checkingRepo: CheckingRepository,
) {
    fun saveChecking(request: List<CheckingRequest>) {
        val list = createCheckingRequestWithSeq(request)
        list.forEach {
            val data = Checking(
                null,
                it.poNumber,
                it.packageCode,
                it.qty,
                it.seqNo,
                it.lotNo,
                it.issueDate
            )
            // save checking
            checkingRepo.save(data)
        }
    }

    fun createCheckingRequestWithSeq(requests: List<CheckingRequest>): List<CheckingRequestWithSeq> {
        return requests
            .groupBy { it.packageCode }
            .flatMap { (packageCode, group) ->
                val latestSeqNo = checkingRepo.findLatestByPackageCode(packageCode!!)?.seqNo ?: 0

                group.mapIndexed { index, checkingRequest ->
                    CheckingRequestWithSeq(
                        poNumber = checkingRequest.poNumber,
                        packageCode = checkingRequest.packageCode,
                        qty = checkingRequest.qty,
                        seqNo = latestSeqNo + index + 1, // Bắt đầu từ latestSeqNo + 1, tăng dần
                        lotNo = checkingRequest.lotNo,
                        issueDate = checkingRequest.issueDate,
                    )
                }
            }
    }
}