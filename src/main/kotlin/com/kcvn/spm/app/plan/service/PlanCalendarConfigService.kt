package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanCalendarConfigModel
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.repository.PlanCalendarConfigRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
@Transactional
class PlanCalendarConfigService(private val planCalendarConfigRep: PlanCalendarConfigRepository) {

    fun getConfigByMonth(date: OffsetDateTime): BaseResponse<PlanCalendarConfigModel> {
        val month = date.month.value
        val year = date.year
        val data = planCalendarConfigRep.getConfigByMonth(month, year) ?: return BaseResponse()

        val response = PlanCalendarConfigModel(
            month = data.month,
            year = data.year,
            startDate = data.startDate,
            endDate = data.endDate,
        )
        return BaseResponse(response)
    }
}