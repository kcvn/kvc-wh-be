package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.WestFactoryLayout
import com.kcvn.spm.model.tables.references.WEST_FACTORY_LAYOUT
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class WestFactoryRepository(private val context: DSLContext) : SortingRepository() {
    fun saveAll(dataList: List<WestFactoryLayout>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    WEST_FACTORY_LAYOUT.newRecord().apply {
                        this.rowNum = data.rowNum
                        this.column1 = data.column1
                        this.column2 = data.column2
                        this.column3 = data.column3
                        this.column4 = data.column4
                        this.column5 = data.column5
                        this.column6 = data.column6
                        this.column7 = data.column7
                        this.column8 = data.column8
                        this.column9 = data.column9
                        this.column10 = data.column10
                        this.column11 = data.column11
                        this.column12 = data.column12
                        this.column13 = data.column13
                        this.column14 = data.column14
                        this.column15 = data.column15
                        this.column16 = data.column16
                        this.column17 = data.column17
                        this.column18 = data.column18
                        this.column19 = data.column19
                        this.column20 = data.column20
                        this.column21 = data.column21
                        this.column22 = data.column22
                        this.column23 = data.column23
                        this.column24 = data.column24
                        this.column25 = data.column25
                        this.column26 = data.column26
                        this.column27 = data.column27
                        this.column28 = data.column28
                        this.column29 = data.column29
                        this.column30 = data.column30
                        this.column31 = data.column31
                        this.column32 = data.column32
                        this.column33 = data.column33
                        this.column34 = data.column34
                        this.column35 = data.column35
                    }
                }
            ).execute()

            result.sum()
        }
    }

    fun delete() {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(WEST_FACTORY_LAYOUT)
                .execute()
        }
    }

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