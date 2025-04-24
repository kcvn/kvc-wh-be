package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.StartActualRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.references.AMOEBA
import com.kcvn.spm.model.tables.references.STOCK_TAKING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class StockTakingRepository(private val context: DSLContext) : SortingRepository() {
    fun copyFromAmoebaToStockTaking(request: StartActualRequest) {
        val year = request.yearNumber
        val month = request.monthNumber

        if (year != null && month != null) {
            val insertQuery = context.insertInto(STOCK_TAKING)
                .columns(
                    STOCK_TAKING.YEAR_NUMBER,
                    STOCK_TAKING.MONTH_NUMBER,
                    STOCK_TAKING.LOCATION_CODE,
                    STOCK_TAKING.PO_NUMBER,
                    STOCK_TAKING.AMOEBA_QTY,
                    STOCK_TAKING.ACTUAL_QTY
                )
                .select(
                    context.select(
                        DSL.`val`(year),
                        DSL.`val`(month),
                        AMOEBA.LOCATION_CODE,
                        AMOEBA.PO_NUMBER,
                        AMOEBA.QTY,
                        DSL.inline(BigDecimal.ZERO)
                    ).from(AMOEBA)
                )

            insertQuery.execute()
        } else {
            throw IllegalArgumentException("yearNumber and monthNumber must not be null")
        }
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "yearNumber" -> STOCK_TAKING.YEAR_NUMBER
            "monthNumber" -> STOCK_TAKING.MONTH_NUMBER
            else -> STOCK_TAKING.YEAR_NUMBER
        }
        return sortField
    }
}