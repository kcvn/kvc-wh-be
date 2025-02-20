package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CancelMoving
import com.kcvn.spm.model.tables.references.CANCEL_MOVING
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class CancelMovingRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(rec: CancelMoving): String? =
        context.insertInto(
            CANCEL_MOVING, CANCEL_MOVING.SOURCE_LOCATION_CODE, CANCEL_MOVING.DEST_LOCATION_CODE,
            CANCEL_MOVING.PO_NUMBER, CANCEL_MOVING.QTY, CANCEL_MOVING.SEQ_NO, CANCEL_MOVING.CREATED_BY)
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(CANCEL_MOVING.ID)
            .fetchOne()?.value1()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> CANCEL_MOVING.DEST_LOCATION_CODE
            "createdDate" -> CANCEL_MOVING.CREATED_DATE
            else -> CANCEL_MOVING.CREATED_DATE
        }
        return sortField
    }
}