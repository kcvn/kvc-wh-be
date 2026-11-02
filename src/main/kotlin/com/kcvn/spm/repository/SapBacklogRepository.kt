package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.StockTakingDailyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.pojos.SapBacklog
import com.kcvn.spm.model.tables.pojos.SendingPickingList
import com.kcvn.spm.model.tables.references.AMOEBA
import com.kcvn.spm.model.tables.references.SAP_BACKLOG
import com.kcvn.spm.model.tables.references.SENDING_PICKING_LIST
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class SapBacklogRepository(private val context: DSLContext) : SortingRepository() {


    fun saveAll(dataList: List<SapBacklog>) {
        if (dataList.isEmpty()) return
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val records = dataList.map { data ->
            context.newRecord(SAP_BACKLOG, data).apply {
                createdBy = user
                createdDate = currentTime
                updatedBy = user
                updatedDate = currentTime
            }
        }
        context.batchInsert(records).execute()
    }

    fun deleteAll() {
        context.deleteFrom(SAP_BACKLOG).execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SAP_BACKLOG.CREATED_DATE
            else -> SAP_BACKLOG.CREATED_DATE
        }
        return sortField
    }
}