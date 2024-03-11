package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanDataByProcessModel
import com.kcvn.spm.app.plan.payload.model.ProcessChildrenModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.OrderFilterType
import com.kcvn.spm.common.constants.PlanTitle
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.repository.PlanRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
@Transactional
class PlanService(private val planRep: PlanRepository) {
    fun getListPlan(request: PlanSearchRequest, pageable: Pageable): BasePagingResponse<ProductPlanModel> {
        val data = planRep.getListPlan(request, pageable)
        val plans = data.first.map { x ->
            ProductPlanModel(
                id = x.id,
                productName = x.productName,
                frame_1 = x.frame_1,
                mold = x.mold,
                pcsSh = x.pcsSh,
                blockSh = x.blockSh
            )
        }
        return BasePagingResponse(plans, data.second)
    }

    fun getPlanDetail(request: PlanDetailRequest): ProductPlanDetailResponse {
        val response = ProductPlanDetailResponse()

        if (request.planProductId.isEmpty()) throw BusinessException("")
        var colStartDate: OffsetDateTime
        var colEndDate: OffsetDateTime

        when(request.filterType) {
            OrderFilterType.DATE -> {
                if (request.startDate == null || request.endDate == null) throw BusinessException("")
                colStartDate = DateTimeHelper.toTimeZone7(request.startDate) ?: OffsetDateTime.now()
                colEndDate = DateTimeHelper.toTimeZone7(request.endDate) ?: OffsetDateTime.now()
            }
            OrderFilterType.ORDER -> {
                val plan = planRep.getPlanByOrderCode(request.orderCode) ?: throw BusinessException("")
                colStartDate = DateTimeHelper.toTimeZone7(plan.startDate) ?: OffsetDateTime.now()
                colEndDate = DateTimeHelper.toTimeZone7(plan.endDate) ?: OffsetDateTime.now()
            }
            else -> throw BusinessException("")
        }

        response.columns = DateTimeHelper.toCalendarColumn(colStartDate, colEndDate)

        val planProcesses = planRep.getListPlanProcess(request.planProductId)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planRep.getPlanDetail(planProcessIds)

        response.data = parentPlanProcess.map { x ->
            val planData = planDetails.filter { m -> m.planProcessId == x.id }
            val productPlan = ProductPlanDetailModel(
                layerCode = x.layerCode,
                processCode = x.processCode,
                processName = x.processName,
                completionRate = x.completionRate,
                processConvertCode = x.processConvertCode,
                inventory = x.inventory
            )
            productPlan.processChildren = childrenPlanProcess.filter { m -> m.parentId == x.id }.map { m ->
                ProcessChildrenModel(
                    layerCode = m.layerCode,
                    processCode = m.processCode,
                    processName = m.processName,
                    inventory = m.inventory
                )
            }
            productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 } ?: 0) + (productPlan.inventory ?: 0)

            for (title in PlanTitle.DATA) {

            }

            productPlan.planData = PlanTitle.DATA.map { m ->
                PlanDataByProcessModel(
                    title = m.value,
                    quantityByCalendars = planData.filter { t -> t.title == m.key }.map { t ->
                        KeyValueResponse(
                            DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                            t.blockQuantity?.toString()
                        )
                    }
                )
            }
            productPlan
        }
        return response
    }
}