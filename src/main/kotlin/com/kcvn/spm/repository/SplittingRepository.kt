package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.model.tables.references.SPLITTING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SplittingRepository(private val context: DSLContext) : SortingRepository() {
    fun findByLocationAndPackage(locationCode: String, packageCode: String): Splitting? {
        return context.selectFrom(SPLITTING)
            .where(
                SPLITTING.LOCATION_CODE.eq(locationCode)
                    .and(SPLITTING.PACKAGE_CODE.eq(packageCode))
            )
            .fetchInto(Splitting::class.java)
            .firstOrNull()
    }

    fun save(data: Splitting): Int? =
        context.insertInto(
            SPLITTING, SPLITTING.RECEIVING_DATE, SPLITTING.PACKAGE_CODE, SPLITTING.LOCATION_CODE, SPLITTING.CREATED_BY)
            .values(data.receivingDate, data.packageCode, data.locationCode, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()

    fun updateLocationCode(oldLocationCode: String, newLocationCode: String, packageCode: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(SPLITTING)
                .set(SPLITTING.LOCATION_CODE, newLocationCode)
                .set(SPLITTING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(SPLITTING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    SPLITTING.PACKAGE_CODE.eq(packageCode)
                    .and(SPLITTING.LOCATION_CODE.eq(oldLocationCode))
                )
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> SPLITTING.LOCATION_CODE
            "createdDate" -> SPLITTING.CREATED_DATE
            else -> SPLITTING.CREATED_DATE
        }
        return sortField
    }
}