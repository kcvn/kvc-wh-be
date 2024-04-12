package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanCalendarConfigModel
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigGetRequest
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigUpdateRequest
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PlanCalendarConfig
import com.kcvn.spm.repository.PlanCalendarConfigRepository
import com.kcvn.spm.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlanCalendarConfigService(
    private val planCalendarConfigRep: PlanCalendarConfigRepository,
    private val planRep: PlanRepository
) {

    fun getConfigByMonth(request: PlanCalendarConfigGetRequest): BaseResponse<PlanCalendarConfigModel> {
        val month = request.date.month.value
        val year = request.date.year
        val data = planCalendarConfigRep.getConfigByMonth(month, year) ?: return BaseResponse()

        val response = PlanCalendarConfigModel(
            month = data.month,
            year = data.year,
            startDate = data.startDate,
            endDate = data.endDate
        )

        if (request.checkVersion == true) {
            val plan = planRep.getByMonth(month, year)
            response.currentPlanVersion = "V${StringHelper.intToStringD2((plan?.version ?: 0))}"
        }

        return BaseResponse(response)
    }

    fun updateConfig(request: PlanCalendarConfigUpdateRequest): BaseResponse<Boolean> {
        val month = request.month.monthValue
        val year = request.month.year

        val data =  PlanCalendarConfig(
            month = month,
            year = year,
            startDate = request.startDate,
            endDate = request.endDate
        )

        val exist = planCalendarConfigRep.getConfigByMonth(month, year)
        if (exist == null) {
            planCalendarConfigRep.add(data)
        } else {
            planCalendarConfigRep.update(data)
        }
        return BaseResponse(true, CommonUtils.getMessage("action.succeeded"))
    }
}