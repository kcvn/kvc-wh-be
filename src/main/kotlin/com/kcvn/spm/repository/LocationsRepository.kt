package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Locations
import com.kcvn.spm.model.tables.references.LOCATIONS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class LocationsRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun findByKeywordPaginated(keyword: String?, pageable: Pageable): Pair<List<Locations>, Int> {
        var condition: Condition = DSL.noCondition()
        if (keyword != null) {
            condition = condition.and(LOCATIONS.LOCATION_CODE.containsIgnoreCase(keyword))
        }
        val roles = context.selectFrom(LOCATIONS).where(condition)
            .orderBy(getSortFields(pageable.sort, LOCATIONS.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(Locations::class.java)
        val total = context.fetchCount(LOCATIONS, condition)
        return Pair(roles, total)
    }

    fun save(location: Locations): String? =
        context.insertInto(LOCATIONS, LOCATIONS.LOCATION_CODE, LOCATIONS.CREATED_BY)
            .values(location.locationCode, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(LOCATIONS.ID)
            .fetchOne()?.value1()

    fun findByName(code: String): Locations? = context.selectFrom(LOCATIONS)
        .where(LOCATIONS.LOCATION_CODE.eq(code))
        .fetchInto(Locations::class.java).firstOrNull()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "code" -> LOCATIONS.LOCATION_CODE
            "createdDate" -> LOCATIONS.CREATED_DATE
            else -> LOCATIONS.CREATED_DATE
        }
        return sortField
    }
}