package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.QuantityReportRequest
import com.kcvn.spm.app.report.quantityreport.payload.response.InformationCalculateQuantityResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantity
import com.kcvn.spm.model.tables.references.CALCULATE_QUANTITY_RESULT
import com.kcvn.spm.model.tables.references.INFORMATION_CALCULATE_QUANTITY
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class QuantityReportRepository(
    private val context: DSLContext,
) : SortingRepository() {
    fun getPagingListQuantityReport(
        request: QuantityReportRequest?,
        pageable: Pageable,
    ): Pair<List<InformationCalculateQuantityResponse>, Int> {
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
        val sortTime = pageable.sort.find { x -> x.property == "timeSort" }
        if(sortTime != null){
            if(sortTime.direction == Sort.Direction.ASC){
                sortFields.add(0, INFORMATION_CALCULATE_QUANTITY.YEAR_NUMBER.asc())
                sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.MONTH_NUMBER.asc())
            } else {
                sortFields.add(0, INFORMATION_CALCULATE_QUANTITY.YEAR_NUMBER.desc())
                sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.MONTH_NUMBER.desc())
            }
        }

//        val sortFields = getSortFields(pageable.sort, INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT).distinct().toMutableList()
//        val sortProductName = pageable.sort.find { x -> x.property == "productName" }
//        if (sortProductName != null){
//                sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT.desc())
//        }
//        val sortMonthReport = pageable.sort.find { x -> x.property == "monthReport" }
//        if (sortMonthReport!=null){
//            sortFields.add(1, INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME.asc())
//        }

        val data = context.select(
            INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME,
            INFORMATION_CALCULATE_QUANTITY.MONTH_NUMBER,
            INFORMATION_CALCULATE_QUANTITY.YEAR_NUMBER,
            INFORMATION_CALCULATE_QUANTITY.TOTAL_QUANTITY_OF_PROCESS,
            INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT,
            INFORMATION_CALCULATE_QUANTITY.PROCESS_STATISTIC,
            INFORMATION_CALCULATE_QUANTITY.CALCULATE_QUANTITY_RESULT_ID,
            CALCULATE_QUANTITY_RESULT.ORDER_DATE_FROM_TO
        )
            .from(INFORMATION_CALCULATE_QUANTITY
                .join(CALCULATE_QUANTITY_RESULT)
                .on(INFORMATION_CALCULATE_QUANTITY.CALCULATE_QUANTITY_RESULT_ID
                    .eq(CALCULATE_QUANTITY_RESULT.ID)))
            .where(condition.and(INFORMATION_CALCULATE_QUANTITY.IS_DELETED.eq(false)))
            .orderBy(sortFields)

            .fetchInto(InformationCalculateQuantityResponse::class.java)

        val total = context.fetchCount(INFORMATION_CALCULATE_QUANTITY, condition.and(INFORMATION_CALCULATE_QUANTITY.IS_DELETED.eq(false)))

        return Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> {
                INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME
            }
            "monthNumber" -> {
                INFORMATION_CALCULATE_QUANTITY.MONTH_NUMBER
            }
            "yearNumber" -> {
                INFORMATION_CALCULATE_QUANTITY.YEAR_NUMBER
            }

            else -> {
                INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME
            }
        }

        return sortField
    }
}