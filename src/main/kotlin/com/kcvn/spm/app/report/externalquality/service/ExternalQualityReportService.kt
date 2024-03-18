package com.kcvn.spm.app.report.externalquality.service

import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExternalQualityReportService {

    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {
        return ExternalQualityReportResponse()
    }

}