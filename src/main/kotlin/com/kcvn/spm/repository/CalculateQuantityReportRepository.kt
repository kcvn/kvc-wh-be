package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantity
import com.kcvn.spm.model.tables.pojos.InformationCalculateQuantityDetail
import com.kcvn.spm.model.tables.references.CALCULATE_QUANTITY_RESULT
import com.kcvn.spm.model.tables.references.INFORMATION_CALCULATE_QUANTITY
import com.kcvn.spm.model.tables.references.INFORMATION_CALCULATE_QUANTITY_DETAIL
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CalculateQuantityReportRepository(
    private val context: DSLContext,
) : SortingRepository() {
    fun findById(request: String): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(request).and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false)))
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun update(data: CalculateQuantityResult?): CalculateQuantityResult? {
        return context.update(CALCULATE_QUANTITY_RESULT)
            .set(CALCULATE_QUANTITY_RESULT.MONTH_REPORT, data?.monthReport)
            .set(CALCULATE_QUANTITY_RESULT.START_DATE, data?.startDate)
            .set(CALCULATE_QUANTITY_RESULT.END_DATE, data?.endDate)
            .set(CALCULATE_QUANTITY_RESULT.STATUS, true)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_BY, data?.calculateBy)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_DATE, data?.calculateDate)
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(data?.id).and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false)))
            .returningResult(CALCULATE_QUANTITY_RESULT)
            .fetchInto(CalculateQuantityResult::class.java).firstOrNull()
    }

    fun findByMonthReport(request: CalculateQuantityRequest): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(
                CALCULATE_QUANTITY_RESULT.START_DATE.ge(request.startDate)
                    .and(CALCULATE_QUANTITY_RESULT.END_DATE.le(request.endDate))
                    .and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            )
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun deleteByIdReport(id: String?) {
        context.deleteFrom(CALCULATE_QUANTITY_RESULT)
            .where(
               CALCULATE_QUANTITY_RESULT.ID.eq(id)
            )
            .execute()
    }



    fun getPagingListCalculateQuantityResult(pageable: Pageable): Pair<List<CalculateQuantityResult>, Int> {
        val data = context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            .orderBy(getSortFields(pageable.sort, CALCULATE_QUANTITY_RESULT.MONTH_REPORT))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(CalculateQuantityResult::class.java)

        val total = context.fetchCount(CALCULATE_QUANTITY_RESULT, CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))

        return Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "monthNumber" -> {
                CALCULATE_QUANTITY_RESULT.MONTH_NUMBER
            }

            "yearNumber" -> {
                CALCULATE_QUANTITY_RESULT.YEAR_NUMBER
            }

            "productName" -> {
                INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME
            }

            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }

        return sortField
    }

    fun addCalculateQuantityResult(
        quantityResult: CalculateQuantityResult,
        listInformationQuantity: List<InformationCalculateQuantity>,
        listInformationCalculateQuantityDetails: List<InformationCalculateQuantityDetail>,
    ) {
        DSL.startTransaction()
        try {
            val quantityResultInsert = context.insertInto(
                CALCULATE_QUANTITY_RESULT,
                CALCULATE_QUANTITY_RESULT.MONTH_REPORT,
                CALCULATE_QUANTITY_RESULT.START_DATE,
                CALCULATE_QUANTITY_RESULT.END_DATE,
                CALCULATE_QUANTITY_RESULT.ORDER_DATE_FROM_TO,
                CALCULATE_QUANTITY_RESULT.CALCULATE_BY,
                CALCULATE_QUANTITY_RESULT.CALCULATE_DATE,
                CALCULATE_QUANTITY_RESULT.UPDATED_DATE,
                CALCULATE_QUANTITY_RESULT.UPDATED_BY,
                CALCULATE_QUANTITY_RESULT.MONTH_NUMBER,
                CALCULATE_QUANTITY_RESULT.YEAR_NUMBER
            ).values(
                quantityResult.monthReport,
                quantityResult.startDate,
                quantityResult.endDate,
                quantityResult.orderDateFromTo,
                quantityResult.calculateBy,
                quantityResult.calculateDate,
                quantityResult.updatedDate,
                quantityResult.updatedBy,
                quantityResult.monthNumber,
                quantityResult.yearNumber
            ).returningResult(CALCULATE_QUANTITY_RESULT).fetchAnyInto(CalculateQuantityResult::class.java)

            if (quantityResultInsert != null) {
                for (informationQuantity in listInformationQuantity) {
                    val informationCalculateQuantityInsert = context.insertInto(
                        INFORMATION_CALCULATE_QUANTITY,
                        INFORMATION_CALCULATE_QUANTITY.MONTH_REPORT,
                        INFORMATION_CALCULATE_QUANTITY.PRODUCT_NAME,
                        INFORMATION_CALCULATE_QUANTITY.PROCESS_STATISTIC,
                        INFORMATION_CALCULATE_QUANTITY.TOTAL_QUANTITY_OF_PROCESS,
                        INFORMATION_CALCULATE_QUANTITY.CREATED_BY,
                        INFORMATION_CALCULATE_QUANTITY.UPDATED_BY,
                        INFORMATION_CALCULATE_QUANTITY.UPDATED_DATE,
                        INFORMATION_CALCULATE_QUANTITY.CALCULATE_QUANTITY_RESULT_ID,
                        INFORMATION_CALCULATE_QUANTITY.MONTH_NUMBER,
                        INFORMATION_CALCULATE_QUANTITY.YEAR_NUMBER
                    ).values(
                        informationQuantity.monthReport,
                        informationQuantity.productName,
                        informationQuantity.processStatistic,
                        informationQuantity.totalQuantityOfProcess,
                        informationQuantity.createdBy,
                        informationQuantity.updatedBy,
                        informationQuantity.updatedDate,
                        quantityResultInsert.id,
                        informationQuantity.monthNumber,
                        informationQuantity.yearNumber
                    ).returningResult(INFORMATION_CALCULATE_QUANTITY)
                        .fetchAnyInto(InformationCalculateQuantity::class.java)

                    if (informationCalculateQuantityInsert != null) {
                        for (informationCalculateQuantityDetail in listInformationCalculateQuantityDetails) {
                            if (informationCalculateQuantityDetail.monthReport == informationCalculateQuantityInsert.monthReport
                                && informationCalculateQuantityDetail.productName == informationCalculateQuantityInsert.productName
                                && informationCalculateQuantityDetail.processStatisticCode == informationCalculateQuantityInsert.processStatistic
                            ) {
                                context.insertInto(
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.MONTH_REPORT,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.PRODUCT_NAME,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.PROCESS_STATISTIC_CODE,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.ORDER_DATE,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.COMPLETION_RATE,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.PROCESS_COUNT,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.BLOCK_QUANTITY,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.BLOCK_SH,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.CREATED_BY,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.QUANTITY_PROCESS_STATISTIC,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.INFORMATION_CALCULATE_QUANTITY_ID,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.MONTH_NUMBER,
                                    INFORMATION_CALCULATE_QUANTITY_DETAIL.YEAR_NUMBER
                                ).values(
                                    informationCalculateQuantityDetail.monthReport,
                                    informationCalculateQuantityDetail.productName,
                                    informationCalculateQuantityDetail.processStatisticCode,
                                    informationCalculateQuantityDetail.orderDate,
                                    informationCalculateQuantityDetail.completionRate,
                                    informationCalculateQuantityDetail.processCount,
                                    informationCalculateQuantityDetail.blockQuantity,
                                    informationCalculateQuantityDetail.blockSh,
                                    informationCalculateQuantityDetail.createdBy,
                                    informationCalculateQuantityDetail.quantityProcessStatistic,
                                    informationCalculateQuantityInsert.id,
                                    informationCalculateQuantityInsert.monthNumber,
                                    informationCalculateQuantityInsert.yearNumber
                                ).returningResult(INFORMATION_CALCULATE_QUANTITY_DETAIL)
                                    .fetchAnyInto(InformationCalculateQuantityDetail::class.java)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            DSL.rollback()
        }
    }


    fun getIdInformationCalculateQuantity(id: String?) : List<String>{
        return context.select(
            INFORMATION_CALCULATE_QUANTITY.ID
        )
            .from(INFORMATION_CALCULATE_QUANTITY)
            .where(INFORMATION_CALCULATE_QUANTITY.CALCULATE_QUANTITY_RESULT_ID.eq(id)
                .and(INFORMATION_CALCULATE_QUANTITY.IS_DELETED.eq(false)))
            .fetchInto(String::class.java)
    }

    fun getIdInformationCalculateQuantityDetail(ids: List<String>) : List<String>{
        return context.select(
            INFORMATION_CALCULATE_QUANTITY_DETAIL.ID
        )
            .from(INFORMATION_CALCULATE_QUANTITY_DETAIL)
            .where(INFORMATION_CALCULATE_QUANTITY_DETAIL.INFORMATION_CALCULATE_QUANTITY_ID.`in`(
                ids
            )
                .and(INFORMATION_CALCULATE_QUANTITY_DETAIL.IS_DELETED.eq(false)))
            .fetchInto(String::class.java)
    }

    fun deleteInformationCalculateQuantityDetail(ids: List<String>) {
         context.deleteFrom(INFORMATION_CALCULATE_QUANTITY_DETAIL)
            .where(INFORMATION_CALCULATE_QUANTITY_DETAIL.ID.`in`(ids))
            .execute()
    }

    fun deleteInformationCalculateQuantity(id: String?){
        context.deleteFrom(INFORMATION_CALCULATE_QUANTITY)
            .where(INFORMATION_CALCULATE_QUANTITY.CALCULATE_QUANTITY_RESULT_ID.eq(id))
            .execute()
    }

    fun getCalculateQuantityResultByMonthReport(month: Int?, year: Int?): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(
                CALCULATE_QUANTITY_RESULT.MONTH_NUMBER.eq(month)
                    .and(CALCULATE_QUANTITY_RESULT.YEAR_NUMBER.eq(year))
                    .and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            )
            .fetchOneInto(CalculateQuantityResult::class.java)
    }


}