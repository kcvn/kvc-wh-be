package com.kcvn.spm.app.transaction.inquiry.service

import com.kcvn.spm.app.transaction.inquiry.payload.request.InquirySearchRequest
import com.kcvn.spm.app.transaction.inquiry.payload.response.InquiryResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.repository.InquiryTransactionsRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InquiryTransactionsService(
    private val inquiryRepo: InquiryTransactionsRepository,
) {
    fun getList(request: InquirySearchRequest, pageable: Pageable): BasePagingResponse<InquiryResponse> {
        val recTrans = inquiryRepo.getList(request, pageable)
        val data = recTrans.first.map {
            InquiryResponse(
                sourceLocationCode = it.sourceLocationCode,
                destLocationCode = it.destLocationCode,
                sourcePackageCode = it.sourcePackageCode,
                destPackageCode = it.destPackageCode,
                poNumber = it.poNumber,
                qty = it.qty,
                seq = it.seqNo,
                transactionType = it.transactionType,
                createdDate = it.createdDate,
                lotNo = it.lotNo,
                issueDate = it.issueDate
            )
        }
        return BasePagingResponse(
            data,
            recTrans.second
        )
    }
}