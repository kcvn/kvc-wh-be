package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.UpdateTape
import com.kcvn.spm.model.tables.references.UPDATE_TAPE
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class UpdateTapeRepository(private val context: DSLContext)  {


    fun getUpdateTapeForReport(updateTapeNames:  List<String>, startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<UpdateTape>{


        var condition = DSL.noCondition().and(UPDATE_TAPE.IS_DELETED.eq(false))

        if (endDate != null) {
            condition= condition.and(UPDATE_TAPE.RESPONSE_DATE.le(endDate))
        }
        if (startDate != null) {
            condition= condition.and(UPDATE_TAPE.RESPONSE_DATE.ge(startDate))
        }

        val data = context.selectFrom(UPDATE_TAPE)
            .where(condition.and(UPDATE_TAPE.GENERIC_NAME.`in`(updateTapeNames)))
            .fetchInto(UpdateTape::class.java)

        return data

    }

}