package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Checking
import com.kcvn.spm.model.tables.references.CHECKING
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class CheckingRepository(private val context: DSLContext) : SortingRepository() {
    fun save(data: Checking): Int? =
        context.insertInto(
            CHECKING, CHECKING.PO_NUMBER, CHECKING.PACKAGE_CODE, CHECKING.QTY, CHECKING.SEQ_NO, CHECKING.CREATED_BY)
            .values(data.poNumber, data.packageCode, data.qty, data.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(CHECKING.SEQ_NO)
            .fetchOne()?.value1()

    fun findLatestByPackageCode(packageCode: String): Checking? {
        return context.selectFrom(CHECKING)
            .where(CHECKING.PACKAGE_CODE.eq(packageCode))
            .orderBy(CHECKING.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(Checking::class.java)
            .firstOrNull()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "poNumber" -> CHECKING.PO_NUMBER
            "createdDate" -> CHECKING.CREATED_DATE
            else -> CHECKING.CREATED_DATE
        }
        return sortField
    }
}