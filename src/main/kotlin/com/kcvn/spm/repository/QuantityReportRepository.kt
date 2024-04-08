package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.QuantityReportRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantity
import com.kcvn.spm.model.tables.references.INFORMATION_CALCULATE_QUANTITY
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class QuantityReportRepository(
    private val context: DSLContext,
) : SortingRepository() {
    fun getPagingListQuantityReport(
        request: QuantityReportRequest?,
        pageable: Pageable,
    ): Pair<List<InformationCalculateQuantity>, Int> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.productName.isNullOrEmpty()) {
                condition = condition.and(DSL.lower(INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME).contains(DSL.lower(request.productName)))
            }
            if (request.startDate != null) {
                condition = condition.and(INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT.ge(request.startDate))
            }
            if (request.endDate != null) {
                condition = condition.and(INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT.le(request.endDate))
            }
        }

        val sortFields = getSortFields(pageable.sort, INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT).distinct().toMutableList()
        val sortProductName = pageable.sort.find { x -> x.property == "productName" }
        if (sortProductName != null){
                sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT.desc())
        }
        val sortMonthReport = pageable.sort.find { x -> x.property == "monthReport" }
        if (sortMonthReport!=null){
            sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME.asc())
        }

        val data = context.selectFrom(INFORMATION_CALCULATE_QUANTITY)
            .where(condition.and(INFORMATION_CALCULATE_QUANTITY.IS_DELETED.eq(false)))
            .orderBy(sortFields)

            .fetchInto(InformationCalculateQuantity::class.java)

        val total = context.fetchCount(INFORMATION_CALCULATE_QUANTITY, condition.and(INFORMATION_CALCULATE_QUANTITY.IS_DELETED.eq(false)))

        return Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> {
                INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME
            }
            "monthReport" -> {
                INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT
            }

            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }

        return sortField
    }
}