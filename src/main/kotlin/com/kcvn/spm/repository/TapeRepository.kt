package com.kcvn.spm.repository

import com.kcvn.spm.app.report.materials.payload.request.GetReportMaterialsRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeInfo
import com.kcvn.spm.model.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TapeRepository (private val context: DSLContext) : SortingRepository()
{
    fun getTapeDetailByMonth (month: Int, year: Int) : TapeInfo? {
        return context.selectFrom(TAPE_INFO)
            .where(TAPE_INFO.MONTH_REPORT.eq(month)
                .and(TAPE_INFO.YEAR_REPORT.eq(year))
                .and(TAPE_INFO.IS_DELETED.eq(false)))
            .fetchAnyInto(TapeInfo::class.java)
    }

    fun deleteTapeByMonth (month: Int?, year: Int?) {
        context.deleteFrom(TAPE_INFO)
            .where(TAPE_INFO.MONTH_REPORT.eq(month)
                .and(TAPE_INFO.YEAR_REPORT.eq(year)))
            .execute()
    }

    fun bulkInsert(request: List<TapeInfo?>){
        val records = request.map { x ->
            DSL.row(
                x?.productName,
                x?.monthReport,
                x?.yearReport,
                x?.requestDateStart,
                x?.requestDateEnd,
                x?.tapeShared,
                x?.typeTape,
                x?.quantityTape,
                x?.unitPrice,
                x?.intoMoney,
                x?.exportType
            )
        }.toTypedArray()

        val insertValuesStep = context.insertInto(
            TAPE_INFO,
            TAPE_INFO.PRODUCT_NAME,
            TAPE_INFO.MONTH_REPORT,
            TAPE_INFO.YEAR_REPORT,
            TAPE_INFO.REQUEST_DATE_START,
            TAPE_INFO.REQUEST_DATE_END,
            TAPE_INFO.TAPE_SHARED,
            TAPE_INFO.TYPE_TAPE,
            TAPE_INFO.QUANTITY_TAPE,
            TAPE_INFO.UNIT_PRICE,
            TAPE_INFO.INTO_MONEY,
            TAPE_INFO.EXPORT_TYPE
        )

        for (record in records) {
            insertValuesStep.values(record)
        }

        insertValuesStep.execute()
    }

    fun getAllReportMaterials(request: GetReportMaterialsRequest, pageable: Pageable) : Pair<List<TapeInfo?>, Int?>{
        var condition: Condition = DSL.noCondition()
        if(!request.productName.isNullOrEmpty()){
            condition = condition.and(DSL.lower(TAPE_INFO.PRODUCT_NAME).contains(DSL.lower(request.productName)))
        }
        if(!request.tapeShared.isNullOrEmpty()){
            condition = condition.and(TAPE_INFO.TAPE_SHARED.eq(request.tapeShared))
        }
        if(!request.typeTape.isNullOrEmpty()){
            condition = condition.and(TAPE_INFO.TYPE_TAPE.eq(request.typeTape))
        }
        if(request.startDate != null){
            val startMonth = request.startDate?.monthValue
            val startYear = request.startDate?.year
            condition = condition.and(TAPE_INFO.MONTH_REPORT.ge(startMonth))
                .and(TAPE_INFO.YEAR_REPORT.ge(startYear))
        }
        if(request.endDate != null){
            val endMonth = request.endDate?.monthValue
            val endYear = request.endDate?.year
            condition = condition.and(TAPE_INFO.MONTH_REPORT.le(endMonth))
                .and(TAPE_INFO.YEAR_REPORT.le(endYear))
        }
        val query = context.selectFrom(TAPE_INFO)
            .where(condition
                .and(TAPE_INFO.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, TAPE_INFO.CREATED_DATE))
            .fetchInto(TapeInfo::class.java)

        val queryTotal = context.selectCount()
            .from(TAPE_INFO)
            .where(condition
                .and(TAPE_INFO.IS_DELETED.eq(false)))
        val totalCount = context.fetchOne(queryTotal)?.value1()
        return Pair(query, totalCount)
    }

    fun checkImportTape (month: Int, year: Int) : TapeInfo? {
        return context.selectFrom(TAPE_INFO)
            .where(TAPE_INFO.MONTH_REPORT.eq(month)
                .and(TAPE_INFO.YEAR_REPORT.eq(year))
                .and(TAPE_INFO.IS_DELETED.eq(false)))
            .fetchAnyInto(TapeInfo::class.java)
    }
    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> {
                TAPE_INFO.PRODUCT_NAME
            }
            "requestDateStart" -> {
                TAPE_INFO.REQUEST_DATE_START
            }
            "processName" -> {
                PROCESS_MASTER.PROCESS_NAME
            }
            "requestDateEnd" -> {
                TAPE_INFO.REQUEST_DATE_END
            }
            "tapeShared" -> {
                TAPE_INFO.TAPE_SHARED
            }
            "typeTape" -> {
                TAPE_INFO.TYPE_TAPE
            }
            "quantityTape" -> {
                TAPE_INFO.QUANTITY_TAPE
            }
            "unitPrice" -> {
                TAPE_INFO.UNIT_PRICE
            }
            "intoMoney" -> {
                TAPE_INFO.INTO_MONEY
            }
            "monthReport" -> {
                TAPE_INFO.MONTH_REPORT
            }
            "yearReport" -> {
                TAPE_INFO.YEAR_REPORT
            }
            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return  sortField
    }
}