package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.model.tables.references.SPLITTING
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class SplittingRepository(private val context: DSLContext) : SortingRepository() {
    fun save(data: Splitting): Int? =
        context.insertInto(
            SPLITTING, SPLITTING.RECEIVING_DATE, SPLITTING.PACKAGE_CODE, SPLITTING.LOCATION_CODE, SPLITTING.CREATED_BY)
            .values(data.receivingDate, data.packageCode, data.locationCode, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        TODO("Not yet implemented")
    }
}