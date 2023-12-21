package com.kcvn.spm.common.repository

import com.kcvn.spm.model.tables.references.AUTH_ROLE
import org.jooq.SortField
import org.jooq.TableField
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Sort

abstract class SortingRepository {
    protected fun getSortFields(sortSpecification: Sort?, defaultSortField: TableField<*, *>?): Collection<SortField<*>> {
        val querySortFields: MutableCollection<SortField<*>> = ArrayList()
        if (sortSpecification == null) {
            if (defaultSortField != null)
                querySortFields.add(defaultSortField.sortDefault())
            return querySortFields
        }
        val specifiedFields: Iterator<Sort.Order> = sortSpecification.iterator()
        while (specifiedFields.hasNext()) {
            val specifiedField: Sort.Order = specifiedFields.next()
            val sortFieldName: String = specifiedField.property
            val sortDirection: Sort.Direction = specifiedField.direction
            val tableField = getTableField(sortFieldName)
            val querySortField = convertTableFieldToSortField(tableField, sortDirection)
            querySortFields.add(querySortField)
        }
        return querySortFields
    }

    protected abstract fun getTableField(sortFieldName: String): TableField<*, *>

    protected fun convertTableFieldToSortField(
        tableField: TableField<*, *>?,
        sortDirection: Sort.Direction
    ): SortField<*> {
        return if (sortDirection === Sort.Direction.ASC) {
            tableField!!.asc()
        } else {
            tableField!!.desc()
        }
    }
}