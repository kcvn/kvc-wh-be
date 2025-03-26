package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.WestFactoryLayout
import com.kcvn.spm.model.tables.references.WEST_FACTORY_LAYOUT
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class WestFactoryRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: BacklogWhSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<WestFactoryLayout>, Int> {
            val query = context.selectFrom(WEST_FACTORY_LAYOUT)
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, WEST_FACTORY_LAYOUT.ROW_NUM))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(WestFactoryLayout::class.java)

            return Pair(data, count)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "rowNum" -> WEST_FACTORY_LAYOUT.ROW_NUM
            else -> WEST_FACTORY_LAYOUT.ROW_NUM
        }
        return sortField
    }
}