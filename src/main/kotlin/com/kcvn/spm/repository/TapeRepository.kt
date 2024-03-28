package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeInfo
import com.kcvn.spm.model.tables.references.*
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
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
                x?.intoMoney
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
            TAPE_INFO.INTO_MONEY
        )

        for (record in records) {
            insertValuesStep.values(record)
        }

        insertValuesStep.execute()
    }
    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "inventoryDate" -> {
                INVENTORY_PRODUCT.INVENTORY_DATE
            }
            "productName" -> {
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE
            }
            "processName" -> {
                PROCESS_MASTER.PROCESS_NAME
            }
            "processCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE
            }
            "layerCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE
            }
            "productQuantity" -> {
                INVENTORY_PRODUCT.PRODUCT_QUANTITY
            }
            "sheetQuantity" -> {
                INVENTORY_PRODUCT.SHEET_QUANTITY
            }
            "pcsSh" -> {
                PRODUCT.PCS_SH
            }
            "orderCode" -> {
                INVENTORY_PRODUCT.ORDER_CODE
            }
            "tapeLotNo" -> {
                INVENTORY_PRODUCT.TAPE_LOT_NO
            }
            "code" -> {
                INVENTORY_PRODUCT.CODE
            }
            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return  sortField
    }
}