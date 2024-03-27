package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeInfo
import com.kcvn.spm.model.tables.references.*
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.stereotype.Repository

@Repository
class TapeRepository (private val context: DSLContext) : SortingRepository()
{
    fun getTapeDetailByMonth (month: String, year: String) : TapeInfo? {
        return context.selectFrom(TAPE_INFO)
            .where(TAPE_INFO.MONTH.eq(month)
                .and(TAPE_INFO.YEAR.eq(year))
                .and(TAPE_INFO.IS_DELETED.eq(false)))
            .fetchAnyInto(TapeInfo::class.java)
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