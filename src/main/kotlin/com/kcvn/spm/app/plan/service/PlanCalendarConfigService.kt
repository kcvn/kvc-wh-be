package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanCalendarConfigModel
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigGetRequest
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigUpdateRequest
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PlanCalendarConfig
import com.kcvn.spm.repository.PlanCalendarConfigRepository
import com.kcvn.spm.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetTime
import java.time.YearMonth
import java.time.ZoneOffset

@Service
@Transactional
class PlanCalendarConfigService(
    private val planCalendarConfigRep: PlanCalendarConfigRepository,
    private val planRep: PlanRepository
) {

    fun getConfigByMonth(request: PlanCalendarConfigGetRequest): BaseResponse<PlanCalendarConfigModel> {
        val month = request.planMonth.split("/")[0].toInt()
        val year = request.planMonth.split("/")[1].toInt()
        val data = planCalendarConfigRep.getConfigByMonth(month, year)

        if (request.hasDefault == true) {
            if (data == null) {
                val preMonth = if (month == 1) 12 else month - 1
                val preYear = if (month == 1) year - 1 else year
                val dataPreMonth = planCalendarConfigRep.getConfigByMonth(preMonth, preYear)
                return if (dataPreMonth == null) {
                    BaseResponse()
                } else {
                    BaseResponse(PlanCalendarConfigModel(
                        month = month,
                        year = year,
                        startDate = dataPreMonth.endDate?.plusDays(1),
                        endDate = YearMonth.of(year, month).atEndOfMonth().atTime(OffsetTime.of(16, 59, 59, 0, ZoneOffset.UTC))
                    ))
                }
            } else {
                return BaseResponse(PlanCalendarConfigModel(
                    month = data.month,
                    year = data.year,
                    startDate = data.startDate,
                    endDate = data.endDate
                ))
            }
        } else {
            if (data == null) return BaseResponse()
            val response = PlanCalendarConfigModel(
                month = data.month,
                year = data.year,
                startDate = data.startDate,
                endDate = data.endDate
            )
            val plan = planRep.getByMonth(month, year)
            response.currentPlanVersion = "V${StringHelper.intToStringD2((plan?.version ?: 0))}"
            return BaseResponse(response)
        }
    }

    fun updateConfig(request: PlanCalendarConfigUpdateRequest): BaseResponse<Boolean> {
        val month = request.planMonth.split("/")[0].toInt()
        val year = request.planMonth.split("/")[1].toInt()

        val data =  PlanCalendarConfig(
            month = month,
            year = year,
            startDate = request.startDate,
            endDate = request.endDate
        )

        val exist = planCalendarConfigRep.getConfigByMonth(month, year)
        if (exist == null) {
            if (planCalendarConfigRep.isOverlap(data))
                throw BusinessException("Khoảng thời gian bị chồng chéo với tháng khác")
            planCalendarConfigRep.add(data)
        } else {
            planCalendarConfigRep.update(data)
        }
        return BaseResponse(true, CommonUtils.getMessage("action.succeeded"))
    }
}